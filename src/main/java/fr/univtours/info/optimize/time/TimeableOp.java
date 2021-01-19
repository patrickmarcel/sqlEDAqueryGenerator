package fr.univtours.info.optimize.time;

/**
 * Any operation whose run time can be estimated and then measured
 */
public interface TimeableOp {
    public long estimatedTime();
    public long actualTime();
}
