## Q1

En mode `naive`, avec les délais par défaut, la scène converge globalement vers un état stable/oscillant (comme attendu), mais avec des anomalies visuelles ponctuelles.

- `update delay` élevé + `refresh delay` normal : évolution lente, plutôt lisible.
- `refresh delay = 0` : artefacts visibles (tearing), scintillement, motifs qui semblent se "déchirer" ou clignoter de manière incohérente.
- `update delay = 0` et `refresh delay` normal : simulation avance vite, l'affichage saute des états (pas de tearing majeur si refresh n'est pas trop agressif).
- les deux à `0` : comportement visuel plus chaotique et instable, avec corruption d'image plus fréquente.

## Q2

La data race principale est entre :

- `updateNext(...)` qui écrit dans `next[r][c]`,
- `refreshCurrent()` qui lit `next[r][c]` pour copier vers `current[r][c]`.

Sans synchronisation, `refreshCurrent()` peut copier `next` pendant qu'un updater est en train de calculer une nouvelle génération partielle. On obtient alors un mélange de cellules issues de deux générations différentes dans `current` (tearing/corruption temporelle).

Deuxième effet : `updateNext(...)` lit `current` pendant que `refreshCurrent()` est en train de réécrire `current`, donc même le calcul de `next` peut utiliser un voisinage incohérent (ni ancienne ni nouvelle génération complète).

Pourquoi c'est pire avec `refresh delay = 0` :

- le thread refresher tourne presque en continu ;
- il interrompt beaucoup plus souvent les phases d'écriture de `next` ;
- la probabilité de copier un état partiel augmente fortement.

## Q3 (bonus)

`LambdaMode` est équivalent à `NaiveMode` fonctionnellement : même logique, syntaxe différente (lambdas au lieu de classes `Runnable`).

## Q4

Implémenté :

- `life.LifeModelSync` (extends `LifeModel`) : override synchronisé des méthodes mutables/accès utiles (`updateNext`, `refreshCurrent`, `isAlive`, `setAlive`, `clear`, `updateFrom`).
- `life.mode.MtSafeMode` : mode `mtsafe` basé sur le démarrage de `NaiveMode` mais en créant un `LifeModelSync`.

## Q5

Effets observés/attendus en `mtsafe` :

- le tearing est fortement réduit (accès exclusifs au modèle) ;
- mais l'alternance update/refresh n'est pas garantie.

Cas limites :

- `update delay = 0`, refresh normal : beaucoup d'updates entre deux refresh, donc ratio `u/r` >> 1 ; l'affichage saute des générations.
- `refresh delay = 0`, update normal : refresh très fréquent d'un même état, ratio `u/r` << 1 ; peu d'évolution visible entre frames.

Pourquoi `synchronized` ne suffit pas :

- `synchronized` garantit exclusion mutuelle + visibilité mémoire ;
- il ne garantit pas l'ordre métier "update puis refresh puis update ...".

## Q6

Implémenté `life.LifeModelBlock` :

- classe thread-safe ;
- alternance stricte avec `wait()/notifyAll()` ;
- booléen d'état `updateTurn` (true au départ) ;
- `updateNext` attend son tour, calcule, passe le tour à refresh ;
- `refreshCurrent` attend son tour, copie/repaint côté mode, passe le tour à update ;
- aucune attente active.

## Q7

Implémenté `life.mode.AlternateMode` (`alternate`) :

- utilise `LifeModelBlock` via `createModel` ;
- thread updater sans sleep ;
- thread refresher avec repaint et sleep configurable.

Avec sliders à 0 :

- alternance conservée ;
- ratio `u/r` proche de 1 ;
- disparition des artefacts principaux.

## Q8

À ce stade, le sleep updater n'a plus d'intérêt :

- l'updater est déjà rythmé par le verrou d'alternance (il bloque tant que refresh n'a pas terminé) ;
- garder un sleep updater ralentit artificiellement toute la simulation.

Le sleep updater a donc été retiré dans `AlternateMode`.

## Q9

Implémenté `life.sync.Turn` :

- état interne `isPlayerOneTurn` (initialisé à `true`) ;
- `startTurn(boolean)` bloquant avec boucle `while` + `wait` ;
- `endTurn()` bascule le tour et fait `notifyAll`.

## Q10

Implémenté `life.mode.ExternalMode` (`external`) :

- modèle inchangé (`LifeModel`) ;
- alternance pilotée par `Turn` (joueur 1 = updater, joueur 2 = refresher) ;
- encadrement des sections par `startTurn(...)` / `endTurn()`.

Résultat attendu : ratio `u/r` proche de 1 et suppression des corruptions visuelles même avec délais à 0.

## Q11

Mode `multi` (N updaters naïfs + 1 refresher) :

- problèmes de race toujours présents ;
- souvent amplifiés avec `N=4` (plus de concurrence d'écriture/lecture sur `next` et `current`).

## Q12 (bonus)

Pourquoi les solutions d'alternance précédentes ne conviennent pas directement :

- `LifeModelBlock` impose une alternance binaire 1 updater / 1 refresher ;
- `Turn` aussi (deux joueurs uniquement) ;
- en multi-updaters, on veut une barrière : "tous les updaters finissent", puis refresh.

## Q13

Implémenté `life.sync.SimpleSemaphore` :

- compteur `permits` ;
- `acquire(int n)` bloquant (`wait`) tant que `permits < n`, puis décrément ;
- `release(int n)` incrément + `notifyAll` ;
- version utilitaire `acquire()` / `release()`.

## Q14

Implémenté `life.mode.TwoSemaphoreMode` (`twosem`) :

- `ready` initialisé à `N` ;
- `done` initialisé à `0` ;
- refresher : `done.acquire(N)`, `refreshCurrent()`, `repaint()`, puis `ready.release(N)` ;
- chaque updater : `ready.acquire()`, `updateNext(...)`, puis `done.release()`.

## Q15 (bonus)

Pourquoi `twosem` est incorrect :

- avec un updater très favorisé, rien n'empêche qu'il reprenne plusieurs permits `ready` sur un même cycle ;
- il peut donc produire plusieurs `done` à lui seul ;
- refresher peut recevoir `N` signaux sans que tous les updaters aient contribué ;
- on casse la contrainte "exactement une contribution par updater et par génération".

## Q16

Implémenté `life.mode.SemaphoreMode` (`semaphore`) :

- un sémaphore `done` à 0 ;
- un tableau `ready[i]` (taille `N`) initialisé à 1 ;
- updater `i` : `ready[i].acquire()`, calcule sa tranche, `done.release()` ;
- refresher : `done.acquire(N)`, refresh/repaint, puis `ready[i].release()` pour tous les `i`.

Cette version force une vraie barrière par génération et évite l'erreur de `twosem`.

## Q17 (bonus)

Méthodologie :

- passer `LifeGame` en grande grille (ex. `800x1200`) ;
- scène aléatoire via reset ;
- sliders à `0` ;
- relever le FPS réel dans l'UI.

Configuration matérielle :

- machine testée : `14` processeurs logiques (`NUMBER_OF_PROCESSORS=14`).

Tableau de mesure conseillé :

| mode | workers | fps |
|---|---:|---:|
| alternate | 1 | ... |
| external | 1 | ... |
| semaphore | 1 | ... |
| semaphore | 2 | ... |
| semaphore | 4 | ... |
| semaphore | 8 | ... |

Conclusion attendue :

- speedup non linéaire (surcoûts synchro + limite affichage Swing) ;
- gain surtout visible entre 1 et quelques threads, puis saturation.

