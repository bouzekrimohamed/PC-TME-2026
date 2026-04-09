# TME 6 / TME 7 - Reponses

## TME 6

### Q1
Execution de `carlvbn.raytracing.Main` OK.  
Le rendu s'affiche dans la fenetre et les temps apparaissent dans la console (`Rendered [...] in Xms`).

### Q2
Dans `Renderer.renderScene(...)`, les points importants sont :
- calcul de `blockSize` a partir de `resolution`
- double boucle sur `x` et `y`
- conversion `(x,y)` -> coordonnees ecran normalisees (`getNormalizedScreenCoordinates`)
- calcul de la couleur (`computePixelInfo`)
- dessin (`setColor` + `fillRect`)

La double boucle calcule donc chaque pixel (ou bloc de pixels) independamment.

### Q3
J'ai ajoute `renderSceneThreadPerPixel(...)` :
- creation d'un `Runnable` par pixel/bloc
- creation d'un thread pour chaque runnable
- `start()` de tous les threads puis `join()` global avant retour

### Q4
Sans protection, l'image a des artefacts.  
Cause : `gfx` est partage par tous les threads et `Graphics` n'est pas thread-safe (data race entre `setColor` et `fillRect`).

Correction appliquee : section critique `synchronized (gfx)`.

### Q5
Probleme de la version per-pixel : trop de threads, donc beaucoup d'overhead.

Pour 1920x1080 en resolution 100% :
- `1920 * 1080 = 2 073 600` threads

J'ai ensuite implemente `renderSceneThreadPerCol(...)` (1 thread par colonne).

### Q6
Mesures (a completer sur la machine de test) :

| Version | Temps (ms) |
|---|---:|
| `renderSceneSequential` | ... |
| `renderSceneThreadPerPixel` | ... |
| `renderSceneThreadPerCol` | ... |

En pratique, `threadPerPixel` est mauvais a cause du nombre de threads.

### Q7
Classe ajoutee : `carlvbn.raytracing.rendering.ThreadPool`.

Implementation :
- file `ArrayBlockingQueue<Runnable>`
- workers persistants
- `execute(r)` fait `put(r)` dans la file
- les workers font `take()` puis `run()`

Autres operations `BlockingQueue` (en plus de `put/take`) :
- `offer`, `offer` avec timeout
- `poll`, `poll` avec timeout
- `add`, `remove`
- `peek`
- `remainingCapacity`
- `drainTo`

### Q8
Ajout d'une instance statique du pool dans `Renderer`, puis version :
- `renderScenePoolCol(...)`

Bonus ajoute :
- `renderScenePoolPixelV2(...)`

### Q9
Attente de fin faite avec `CountDownLatch` :
- latch initialise avec le nombre de taches
- chaque tache appelle `countDown()` en `finally`
- le thread principal appelle `await()`

### Q10
La contention vient du `synchronized(gfx)` :  
les threads calculent en parallele, mais le dessin est serialise dans une section critique.

### Q11
Pour enlever cette contention, j'ai utilise une `BufferedImage` partagee :
- ecriture parallele via `fillColorRect(...)` sur zones disjointes
- puis un seul `gfx.drawImage(...)` a la fin

Versions :
- `renderScenePoolColV2(...)`
- `renderScenePoolPixelV2(...)`

Comparaison (a completer) :

| Version | Temps (ms) |
|---|---:|
| `pool-col-sync` | ... |
| `pool-col-image` | ... |
| `pool-pixel-image` | ... |

### Q12 (bonus)
Tests proposes :
- taille de file : `10`, `100`, tres grande
- nombre de threads : `1`, `nb coeurs`, `2x nb coeurs`, `200`

Observation generale :
- file trop petite => blocages plus frequents
- trop de threads => surcout d'ordonnancement
- meilleur compromis proche du nombre de coeurs

Configuration retenue pour le code :
- `POOL_THREADS = Runtime.getRuntime().availableProcessors()`
- `POOL_QUEUE_CAPACITY = 100`

## TME 7

### Q1
Remplacement par `ExecutorService` dans `Renderer` :
- `newSingleThreadExecutor`
- `newFixedThreadPool(...)`
- mode per-task (simulation compatible Java 8)

Version utilisee :
- `renderSceneExecutorCol(...)`

Mesures (a completer) :

| Executor | Temps (ms) |
|---|---:|
| single | ... |
| fixed | ... |
| per-task | ... |

### Q2
Executors testes :
- single thread
- fixed thread pool
- thread par tache (simulation)

Note :
- `newVirtualThreadPerTaskExecutor` n'est pas disponible en Java 8.

### Q3 (bonus)
Pistes d'experiences :
1. `invokeAll` vs `execute + CountDownLatch`
2. `BlockingQueue` JDK vs file maison
3. `CountDownLatch` JDK vs latch maison
4. granularite des taches (pixel, colonne, bloc)
