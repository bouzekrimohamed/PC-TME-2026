Complétez avec vos réponses.

# Q1

Mode: hash, fichier: data/WarAndPeace.txt, N=4
Total runtime: 282 ms
Total words: 565527
Unique words: 20332
Top 5:
34562 the
22148 and
16709 to
14990 of
10513 a

q2

21311477@ppti-24-301-16:~/git/PC-TME-2026/tme2$ javac -d bin $(find src -name "*.java")
21311477@ppti-24-301-16:~/git/PC-TME-2026/tme2$ java -cp bin pc.WordFrequency data/WarAndPeace.txt hash2 4
Preparing to parse data/WarAndPeace.txt (mode=hash2, N=4), containing 3235342 bytes
Total words: 565527
Unique words: 20332
34562 the
22148 and
16709 to
14990 of
10513 a
Total runtime: 292 ms for mode hash2


donc 
hash : 282 ms
hash2 : 292 ms


q8

partition (N=4) : 306 ms
Résultats corrects (Total words 565527 / Unique 20332)

Q13, N=4

shard (N=4) : 186 ms

Résultats corrects (Total words 565527 / Unique 20332)


Résultats tous corrects Total words=565527,Unique=20332 :
N=1 → Total runtime: 292 ms
N=2 → Total runtime: 208 ms
N=4 → Total runtime: 188 ms (meilleur)
N=6 → Total runtime: 210 ms
N=8 → Total runtime: 205 ms
N=16 → Total runtime: 197 ms
Observation : le gain est net jusqu’à 4 threads, puis les performances stagnentse dégradent légérement


Q14 — Meilleur N et lien avec le matériel

Sur ma machine, le meilleur temps observé pour shard est obtenu avec N = 4 (≈ 188 ms)
Matériel (lscpu) : Intel Core i7-13700, 1 socket, 16 cœurs physiques, 2 threads par cœur ------> 32 threads logiques

On s’intéresse au nombre de cœurs car le gain du parallélisme est limité par le parallélisme matériel : au-delà d’un certain nombre de threads, on augmente surtout la surcharge (création/scheduling des threads, contention cache/mémoire, fusion des maps), donc les performances stagnent ou se dégradent



Q11 (bonus):
Complexité de mergeInto(destination, source) : on parcourt toutes les entrées de source (taille |S|) et pour chacune on fait un get/put en O(1) amorti dans une HashMap, donc O(|S|) amorti.

Si on inverse source/destination, le coût dépend surtout de la taille de la source, donc on veut que la source soit la plus petite 
