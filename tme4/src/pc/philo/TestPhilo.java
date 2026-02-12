package pc.philo;

public class TestPhilo {

	public static void main (String [] args) throws InterruptedException{
		final int NB_PHIL = 5;
		Thread [] tPhil = new Thread[NB_PHIL];
		Fork [] tChop = new Fork[NB_PHIL];

		 for (int i = 0; i < NB_PHIL; i++) {
	            tChop[i] = new Fork();
	        }

	        // crer et lance les philosophes
	        for (int i = 0; i < NB_PHIL; i++) {
	            Fork left = tChop[i];
	            Fork right = tChop[(i + 1) % NB_PHIL];

	            // Q5 : le philosophe NB_PHIL-1 a (left=NB_PHIL-1, right=0) -> ordre "naturel" casser
	            // Correction : on inverse pour celui-là pour casser le cycle dattente (evite deadlock).
	            if (i == NB_PHIL - 1) {
	                Fork tmp = left;
	                left = right;
	                right = tmp;
	            }

	            Philosopher p = new Philosopher(left, right);
	            tPhil[i] = new Thread(p, "Philo-" + i);
	            tPhil[i].start();
	        }

	        // Q6 : laisse tourner quelques secondes
	        Thread.sleep(3000);

	        // Q6 : interruption
	        for (Thread t : tPhil) {
	            t.interrupt();
	        }

	        // Q6 : attendre la fin propre
	        for (Thread t : tPhil) {
	            t.join();
	        }
		
		System.out.println("Fin du programme");

	}
}