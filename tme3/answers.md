Complétez avec vos réponses.

Q1

Mode hash, fichier WarAndPeace.txt, N=4

Temps : 282 ms

Résultats :

Total words : 565527

Unique words : 20332

Top 5 :

34562 the
22148 and
16709 to
14990 of
10513 a



Ces valeurs servent de référence pour vérifier que les autres modes donnent le bon résultat.

Q2

Comparaison entre hash et hash2 :

hash : 282 ms

hash2 : 292 ms

Les résultats sont les mêmes (même nombre de mots et de mots uniques).
Les performances sont très proches, Temps très proches, pas de gain notable

Q8

Mode partition (N=4)

Temps : 306 ms

Résultats corrects :

Total words : 565527

Unique words : 20332

Ce mode reste séquentiel, donc il n’apporte pas vraiment d’amélioration de performance.

Q13

Mode shard avec différents nombres de threads :

N	Temps
1	292 ms
2	208 ms
4	188 ms
6	210 ms
8	205 ms
16	197 ms

Les résultats sont corrects dans tous les cas.

On observe une amélioration jusqu’à 4 threads, puis les performances n’augmentent plus vraiment.

Q14

Le meilleur temps est obtenu pour N = 4 (~188 ms).

Machine utilisée :

Intel i7-13700

16 cœurs physiques, 32 threads logiques

Le gain du parallélisme est limité par le matériel.
Quand on utilise trop de threads, il y a plus de surcharge (gestion des threads, accès mémoire, fusion des résultats), donc les performances stagnent ou diminuent un peu.

Q7

La version proposée n’est pas correcte.

Même si chaque accès est synchronisé, la suite d’opérations :

get --> test --> put


n’est pas atomique.

Deux threads peuvent lire la même valeur et écrire ensuite un résultat incorrect (lost update).

Correction :

synchronized(map) {
    Integer count = map.get(word);
    if (count == null)
        map.put(word, 1);
    else
        map.put(word, count + 1);
}

Q10

Même problème avec Collections.synchronizedMap.

Chaque appel (get ou put) est synchronisé, mais la séquence complète ne l’est pas.
Il peut donc encore y avoir des lost updates.

La solution est de synchroniser toute la séquence :

synchronized(map) {
    Integer count = map.get(word);
    if (count == null)
        map.put(word, 1);
    else
        map.put(word, count + 1);
}
