package esq.phonemod.api.v3;

import javax.annotation.Nonnull;
import java.util.Set;

/**
 * Fixture app compiled solely against {@code phonemod-api} (R-API-3): it never
 * imports anything from {@code esq.phonemod.phone.*} runtime internals or any
 * UI type. Used by {@code ApiContractTest.sampleAppCompilesAgainstApiOnly}.
 */
public final class HelloContractApp implements DeviceApp<HelloContractApp.Model> {

    public static final class Model extends ViewModel {
        public final Prop<String> greeting = prop("Greeting", "hello");
    }

    @Override
    @Nonnull
    public AppDescriptor descriptor() {
        return AppDescriptor.builder("esq.hello:hello")
                .name("Hello")
                .permissions(Set.of())
                .build();
    }

    @Override
    @Nonnull
    public Model createModel(@Nonnull DeviceAppContext<Model> context) {
        return new Model();
    }

    @Override
    public void onOpen(@Nonnull DeviceAppContext<Model> context, @Nonnull Model model) {
    }

    @Override
    public void onAction(@Nonnull DeviceAppContext<Model> context, @Nonnull Model model,
            @Nonnull DeviceAction action) {
    }
}
