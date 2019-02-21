package magnet.internal;

import java.util.HashMap;
import java.util.Map;
import test.Interface7Implementation7MagnetFactory;

@Generated
public final class MagnetIndexer {
    public static void register(MagnetInstanceManager instanceManager) {
        InstanceFactory[] factories = new InstanceFactory[] {
            new Interface7Implementation7MagnetFactory(),
        };
        Map<Class, Object> index = new HashMap<>(16);
        index.put(Interface7Implementation7MagnetFactory.getType(), new Range(0, 1, ""));
        instanceManager.register(factories, index);
    }
}