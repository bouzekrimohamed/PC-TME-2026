# Question 1

Complexité de "repeat" la version fournie qui utilise String ?
À chaque itération, Java crée une nouvelle String et recopie l’ancienne + le nouveau char.

Cout ≈ 1 + 2 + 3 + … + n = O(n²)

O(n²) en temps (et O(n) mémoire pour la string finale, mais beaucoup d’allocs temporaires).


# Question 2


Temps version initiale (String) :
425 ms

Temps version StringBuilder() :
1 ms

Temps version StringBuilder(n) :
1 ms



# Question 3

Nombre de réallocations en fonction de N ?

- Avec new StringBuilder() : la capacité grandit par paliers (croissance multiplicative),
  donc le nombre de réallocations est en O(log N).
  
- Avec new StringBuilder(n) : en pratique 0 réallocation (on réserve directement N).

Complexité de "repeat" avec StringBuilder() ?
- O(N) en temps (amorti), O(N) en mémoire.

Complexité de "repeat" avec StringBuilder(n) ?
- O(N) en temps, O(N) en mémoire, et généralement plus rapide car évite les réallocations/recopies.



# Question 4

N,Strategy,Time(us)
100,naive,13541
1000,naive,693
10000,naive,9093
50000,naive,112540
100000,naive,379251
200000,naive,1464028
300000,naive,3271249
100,builder_default,74
1000,builder_default,108
10000,builder_default,309
50000,builder_default,646
100000,builder_default,777
200000,builder_default,1296
300000,builder_default,1185
100,builder_capacity,2
1000,builder_capacity,13
10000,builder_capacity,124
50000,builder_capacity,627
100000,builder_capacity,707
200000,builder_capacity,1274
300000,builder_capacity,1910


Loaded 21 measures from benchmark.csv
- naive: N from 100 to 300000, time from 13541us to 3271249us
- builder_default: N from 100 to 300000, time from 74us to 1185us
- builder_capacity: N from 100 to 300000, time from 2us to 1910us

Summary of Benchmark Data:

Key:
naive: String only
default: StringBuilder()
capacity: StringBuilder(n)

Naive does not fit a linear regression (R²=0.91), but fits quadratic well (R²=1.00).
Linear regression fits default to 5.10 ns/char (R²=0.66).
Linear regression fits capacity to 6.53 ns/char (R²=0.97).

Speed of strategies in ns/char:
naive: 909.30 ns/char at 10⁴, 3792.51 ns/char at 10⁵, 10904.16 ns/char at 3×10⁵
default: 30.90 ns/char at 10⁴, 7.77 ns/char at 10⁵, 3.95 ns/char at 3×10⁵
capacity: 12.40 ns/char at 10⁴, 7.07 ns/char at 10⁵, 6.37 ns/char at 3×10⁵

Ratios:
naive/default: 29.43 at 10⁴, 488.10 at 10⁵, 2760.55 at 3×10⁵
naive/capacity: 73.33 at 10⁴, 536.42 at 10⁵, 1712.70 at 3×10⁵
default/capacity: 2.49 at 10⁴, 1.10 at 10⁵, 0.62 at 3×10⁵

Successfully produced file benchmark.pdf



Le benchmark confirme expérimentalement les complexités théoriques.
La stratégie naïve utilisant la concaténation de chaînes (`String +=`) ne suit
pas un comportement linéaire (R² = 0.91) mais correspond très bien à une loi
quadratique (R² = 1.00). Cela s’explique par l’immutabilité des objets `String`,
chaque concaténation entraînant la création et la copie complète d’une nouvelle chaîne.

Les versions utilisant `StringBuilder` présentent un comportement proche de O(n).
Les temps par caractère restent faibles et relativement stables lorsque N augmente.
La version `StringBuilder(n)` est globalement la plus efficace, car la capacité
initiale évite les réallocations internes du buffer, ce qui améliore les constantes
de temps.

On observe également un écart de performance croissant entre la version naïve
et les versions `StringBuilder` : pour N = 3×10⁵, la version naïve est plus de
2000 fois plus lente que les versions optimisées.

Copiez la trace du script python.

Commentez vos résultats du benchmark


# Exercice II : fréquence des mots


