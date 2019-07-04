package magnet.internal;

import magnet.Classifier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class InstanceBucketTest {

    @Mock private Factory1 factory1;
    @Mock private Factory2 factory2;

    @Mock private Interface1 instance1;
    @Mock private Interface1 instance2;

    @Mock private InstanceBucket.OnInstanceListener listener;
    @Mock private MagnetScope scope;

    @Test
    @SuppressWarnings("unchecked")
    public void test_getScope() {
        InstanceBucket<String> instances = new InstanceBucket(
            scope, factory1, Interface1.class, instance1, Classifier.NONE, listener
        );
        assertThat(instances.getScope()).isSameAs(scope);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test_getInstances_SingleItem() {
        InstanceBucket<Interface1> instances = new InstanceBucket(
            scope, factory1, Interface1.class, instance1, Classifier.NONE, listener
        );
        List<Interface1> result = instances.getMany();
        assertThat(result).containsExactly(instance1);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test_getInstances_ManyItems() {
        InstanceBucket<Interface1> instances = new InstanceBucket(
            scope, factory1, Interface1.class, instance1, Classifier.NONE, listener
        );
        instances.registerObject(factory2, Interface1.class, instance2, Classifier.NONE);
        List<Interface1> result = instances.getMany();
        assertThat(result).containsExactly(instance1, instance2);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test_getSingleInstance() {
        InstanceBucket<Interface1> instances = new InstanceBucket(
            scope, factory1, Interface1.class, instance1, Classifier.NONE, listener
        );
        Interface1 result = instances.getSingleInstance();
        assertThat(result).isSameAs(instance1);
    }

    interface Interface1 {}
    abstract static class Factory1 extends InstanceFactory<Interface1> {}
    abstract static class Factory2 extends InstanceFactory<Interface1> {}
}
