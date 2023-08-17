package pac.inst;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface InstrumentationMethod {

  InvocationType invocationType() default InvocationType.VIRTUAL;

  InstrumentationType instrumentationType() default InstrumentationType.REPLACE;

  InstrumentationLocation instrumentationLocation() default InstrumentationLocation.ALL;

  String name() default "";

  String descriptor() default "";

  String skippedDescriptor() default "";

  boolean canExtend() default false;

  boolean inline() default false;

}
