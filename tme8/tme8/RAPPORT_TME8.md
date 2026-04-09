# TME 8 - Programmation Concurrente

## Informations

- `Mohamed Bouzekri`
-  `tme8`
-`Java`

## 3. Web Crawler

### Question 1 - `WebCrawlerParallel`

La classe `WebCrawlerParallel` a ete implementee dans `src/pc/crawler/WebCrawlerParallel.java` avec:

- une file bloquante partagee `BlockingQueue<CrawlTask>` contenant `(url, profondeur)`;
- un pool de threads (`ExecutorService`) pour executer les workers;
- un worker qui:
  1. prend une tache depuis la file,
  2. traite l'URL avec `WebCrawlerUtils.processUrl(...)`,
  3. ajoute les liens extraits avec profondeur decrementee.

La profondeur est decrementee a chaque niveau:

- si `depth > 0`, on ajoute les enfants en `depth - 1`;
- si `depth == 0`, on ne propage plus les liens.

### Question 2 - Gestion des URLs deja visitees

Une `ConcurrentHashMap<String, Boolean>` est utilisee pour eviter les cycles et le retraitement:

- insertion atomique avec `putIfAbsent(url, TRUE)`;
- une URL n'est enfilee que si elle n'a jamais ete vue.

Cela garantit que:

- plusieurs threads ne crawleront pas la meme page;
- les cycles de liens ne provoquent pas de boucle infinie.

### Question 3 - Classe `ActivityMonitor`

La classe `ActivityMonitor` est implemente dans `src/pc/crawler/ActivityMonitor.java` avec:

- un `AtomicInteger counter`;
- `taskStarted()` : incremente;
- `taskCompleted()` : decremente et `notifyAll()` si compteur a 0;
- `awaitCompletion()` : attend tant que `counter != 0`.

### Question 4 - Terminaison propre (poison pills)

`WebCrawlerParallel` a ete modifie pour utiliser `ActivityMonitor`:

- `taskStarted()` est appele **avant chaque `queue.put(...)`** (y compris URL initiale);
- `taskCompleted()` est appele en `finally` apres traitement complet d'une URL;
- le thread principal appelle `awaitCompletion()`;
- quand toutes les taches sont terminees, il injecte des `poison pills` (une par worker);
- chaque worker s'arrete des qu'il lit une poison pill.

Cette strategie evite les deadlocks de terminaison et garantit l'arret des workers.

### Question 5 - Mesures de performance

Le temps est mesure dans `WebCrawlerParallel` avec:

- `long startNanos = System.nanoTime();`
- affichage final `elapsed=... ms`

Commande generique:

```powershell
java -cp src pc.crawler.WebCrawlerParallel <depth> <workers>
```

Mesures relevees (machine locale):

| Profondeur | Workers | Temps (ms) |
|---|---:|---:|
| 0 | 1 | 1069 |
| 1 | 2 | 7720 |
| 1 | 4 | 2663 |

Remarques experimentales:

- les temps varient selon la charge reseau et le cache local (`tmp/crawler`);
- en profondeur 2, le nombre de pages augmente fortement (explosion combinatoire), ce qui allonge tres nettement l'execution.

## 4. Thumbnail

### Question 1 - Version pipeline parallele

Une version pipeline a ete proposee dans `src/pc/thumbnail/ImageResizerPipelineApp.java`:

- etape 1 (`loader`): lecture des images;
- etape 2 (`resizer`): redimensionnement;
- etape 3 (`saver`): sauvegarde.

Communication inter-etapes:

- `BlockingQueue<File>` puis `BlockingQueue<LoadedImage>` puis `BlockingQueue<ResizedImage>`;
- terminaison par sentinelles (*poison messages*).

Cette architecture suit le modele pipeline vu en TD.

## Compilation et execution

Compilation:

```powershell
javac src/pc/crawler/*.java src/pc/thumbnail/*.java
```

Execution crawler parallele:

```powershell
java -cp src pc.crawler.WebCrawlerParallel 1 4
```

Execution thumbnail pipeline:

```powershell
java -cp src pc.thumbnail.ImageResizerPipelineApp
```

