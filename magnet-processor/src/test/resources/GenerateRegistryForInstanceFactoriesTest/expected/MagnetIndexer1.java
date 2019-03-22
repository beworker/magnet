package magnet.internal;

import java.util.HashMap;
import java.util.Map;
import test.Implementation1MagnetFactory;

@Generated
public final class MagnetIndexer {
    public static void register(MagnetInstanceManager instanceManager) {
        InstanceFactory[] factories = new InstanceFactory[] {
            new Implementation1MagnetFactory(),
        };
        Map<Class, Object> index = new HashMap<>(16);
        index.put(Implementation1MagnetFactory.getType(), new Range(0, 1, ""));
        instanceManager.register(factories, index);
    }
}