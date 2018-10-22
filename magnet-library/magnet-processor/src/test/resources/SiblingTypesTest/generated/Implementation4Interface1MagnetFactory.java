package siblings;

import magnet.Scope;
import magnet.Scoping;
import magnet.internal.InstanceFactory;

public final class Implementation4Interface1MagnetFactory extends InstanceFactory<Interface1> {
    private static Class[] SIBLING_TYPES = { Interface2.class };

    @Override
    public Interface1 create(Scope scope) {
        return new Implementation4();
    }

    @Override
    public Scoping getScoping() {
        return Scoping.TOPMOST;
    }

    @Override
    public Class[] getSiblingTypes() {
        return SIBLING_TYPES
    }

    public static Class getType() {
        return Interface1.class;
    }
}
