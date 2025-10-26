import annotation.Controller;
import annotation.TestController1;
import annotation.TestController2;
import annotation.TestController3;

public class Main {
    public static void main(String[] args) {
        // Main method can be used for testing if needed
        Object controller1 = new TestController1();
        verifController(controller1);
        Object controller2 = new TestController2();
        verifController(controller2);
        Object controller3 = new TestController3();
        verifController(controller3);
    }

    public static void verifController(Object controller) {
        Class<?> controllerClass = controller.getClass();
        boolean hasAnnotation = controllerClass.isAnnotationPresent(Controller.class);
        if (hasAnnotation) {
            System.out.println("The class " + controllerClass.getName() + " is annotated with @Controller.");
        } else {
            System.out.println("The class " + controllerClass.getName() + " is NOT annotated with @Controller.");
        }
    }


}
