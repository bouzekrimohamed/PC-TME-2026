
# completer ce fichier avec vos réponses aux questions sans code

Q4 (problème rencontré) : deadlock possible : chacun prend sa baguette gauche et attend la droite, plus personne n’avance.

Q7 (interrupt + deadlock) : si tu utilises lock() classique, un thread bloqué sur le lock ne réagit pas à interrupt(). Solution : utiliser lockInterruptibly() (comme dans le Fork.acquire() ci-dessus).