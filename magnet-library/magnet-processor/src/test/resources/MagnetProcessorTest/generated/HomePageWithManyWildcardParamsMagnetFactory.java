package app.extension;

import app.HomeRepository;
import app.Page;
import java.util.List;
import magnet.Scope;
import magnet.Scoping;
import magnet.internal.InstanceFactory;

public final class HomePageWithManyWildcardParamsMagnetFactory extends InstanceFactory<Page> {

    @Override
    public Page create(Scope scope) {
        List<HomeRepository> repositories = scope.getMany(HomeRepository.class);
        return new HomePageWithManyWildcardParams(repositories);
    }

    @Override
    public Scoping getScoping() {
        return Scoping.TOPMOST;
    }

    public static Class getType() {
        return Page.class;
    }
}