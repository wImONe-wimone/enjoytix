package com.wimone.enjoytix.agent.remote;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.lang.reflect.Method;
import java.util.Arrays;
import static org.assertj.core.api.Assertions.assertThat;
class ReadOnlyRemoteContractTest {
 @Test void allAgentRemoteMethodsAreGetOnly() {
  for (Class<?> type : new Class<?>[]{PerformanceRemoteService.class, TicketReadRemoteService.class, OrderReadRemoteService.class, UserReadRemoteService.class, CommentReadRemoteService.class})
   assertThat(Arrays.stream(type.getMethods()).filter(m -> m.getDeclaringClass()==type).allMatch(m -> m.isAnnotationPresent(GetMapping.class))).isTrue();
 }
 @Test void orderQueriesHaveNoClientSuppliedUserId() {
  for (Method method: OrderReadRemoteService.class.getDeclaredMethods()) {
   assertThat(Arrays.stream(method.getParameterAnnotations()).flatMap(Arrays::stream).noneMatch(a -> a instanceof RequestParam p && p.name().equals("userId"))).isTrue();
   assertThat(Arrays.stream(method.getParameterAnnotations()).flatMap(Arrays::stream).noneMatch(a -> a instanceof PathVariable p && p.name().equals("userId"))).isTrue();
  }
 }
 @Test void userQueryIsFixedToCurrentUserEndpoint() throws Exception {
  assertThat(UserReadRemoteService.class.getDeclaredMethod("me").getAnnotation(GetMapping.class).value()).containsExactly("/api/user/me");
 }
}
