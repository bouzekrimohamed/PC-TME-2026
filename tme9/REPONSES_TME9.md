# TME 9 - Programmation Concurrente (ForkJoin)

## Question 1 - Mandelbrot parallèle

Implémentation réalisée dans `src/pc/mandelbrot/MandelbrotCalculator.java` :
- classe interne `MandelbrotTask extends RecursiveAction`
- découpage horizontal via `startY` / `endY`
- seuil `THRESHOLD = 5000` pixels
- cas de base : calcul séquentiel de la zone
- sinon : division en 2 sous-tâches avec `invokeAll(...)`
- `parCompute(...)` lance la tâche racine avec `ForkJoinPool.commonPool()`

Pourquoi ForkJoin est adapté ici :
- chaque pixel est indépendant, donc parallélisable facilement
- le coût par pixel n'est pas uniforme (certaines zones divergent vite, d'autres non)
- le work-stealing équilibre bien la charge entre threads

## Question 2 - Analyse des performances

- **Nombre de threads** : les performances montent jusqu'à un plateau proche du nombre de cœurs utiles ; au-delà, le surcoût de scheduling augmente.
- **Taille image** : plus l'image est grande, plus le gain parallèle est visible (surcoût ForkJoin amorti).
- **maxIterations** : augmente le coût de chaque pixel, donc augmente en général l'intérêt du parallélisme.

Pourquoi le speedup n'est pas linéaire :
- overhead de gestion des tâches
- déséquilibre de charge (zones de l'image plus coûteuses)
- limites matérielles (cache, bande passante mémoire, contention)

## Question 3 - QuickSort parallèle

Implémentation réalisée dans `src/pc/quicksort/QuickSort.java` :
- `QuickSortTask extends RecursiveAction`
- logique : seuil `THRESHOLD`, sinon partition puis 2 sous-tâches
- méthode `parQuickSort(int[] array)` avec `commonPool`

## Question 4 - Nombre de threads

Une version configurable est fournie :
- `parQuickSort(int[] array, int parallelism)`
- création d'un pool dédié : `new ForkJoinPool(parallelism)`

Utiliser un pool dédié est préférable quand :
- on veut contrôler précisément les ressources CPU
- plusieurs traitements parallèles coexistent dans le même programme
- on veut des mesures plus stables pour comparer les performances

## Question 5 - Impact du threshold

- **Threshold trop petit** : trop de tâches -> overhead important.
- **Threshold trop grand** : pas assez de tâches -> parallélisme sous-exploité.

