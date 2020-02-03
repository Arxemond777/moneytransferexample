package mondeytransfer.other;

import java.util.concurrent.ThreadFactory;

/**
 * @author <a href="mailto:1arxemond1@gmail.com">Yuri Glushenkov</a>
 */
public class CustomThreadFactory implements ThreadFactory {
    private int counter;
    private String name;

    public CustomThreadFactory(String name) {
        counter = 0;
        this.name = name;
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Thread t = new Thread(runnable, name + counter);
        counter++;
        return t;
    }
}
