import fr.i360matt.tweakedrunnable.TweakedRunnable;

public class Test {

    public static void main (String[] args) {


        TweakedRunnable alpha = TweakedRunnable.create(() -> {
            System.out.println("A");
            Thread.sleep(1_000);
        });

        alpha.setThreads(1);


        TweakedRunnable beta = TweakedRunnable.create(() -> {
            System.out.println("B");
            Thread.sleep(1_000);
        });
        beta.setThreads(10);


        long start = System.nanoTime();
        for (int i = 0; i < 10_000; i++) {
            alpha.runAsync();
            beta.runAsync();
        }
        long end = System.nanoTime();

        System.out.println((end - start) + "ns");



    }

}
