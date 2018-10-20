package app.extension;

import magnet.Scope;
import magnet.Scoping;
import magnet.internal.InstanceFactory;

public final class ExecutorImplMagnetFactory extends InstanceFactory<Executor> {
    @Override
    public Executor create(Scope scope) {
        return new ExecutorImpl();
    }

    @Override
    public Scoping getScoping() {
        return Scoping.TOPMOST;
    }

    public static Class getType() {
        return Executor.class;
    }
}