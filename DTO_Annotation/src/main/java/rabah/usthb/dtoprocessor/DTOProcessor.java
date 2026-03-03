package rabah.usthb.dtoprocessor;

import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import java.io.IOException;
import java.io.Writer;

import java.util.Map;
import java.util.Set;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;


@AutoService(Processor.class)
@javax.annotation.processing.SupportedAnnotationTypes({"rabah.usthb.dtoprocessor.DTO" , "rabah.usthb.dtoprocessor.DTOField" })
@javax.annotation.processing.SupportedSourceVersion(javax.lang.model.SourceVersion.RELEASE_17)
public class DTOProcessor extends AbstractProcessor {

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        if (annotations.isEmpty()) {
            return false;
        }

        String nameDTO = "";
        StringBuilder fields = new StringBuilder();

        for (TypeElement annotation : annotations) {


            if (annotation.getSimpleName().toString().equals("DTO")) {


                for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                    System.err.println("Processing: " + element.getSimpleName());

                    for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
                        for (Map.Entry<? extends javax.lang.model.element.ExecutableElement, ? extends javax.lang.model.element.AnnotationValue> entry : mirror.getElementValues().entrySet()) {
                            System.err.println("KEY " + entry.getKey().getSimpleName() + " VALUE : " + entry.getValue().getValue());
                            if (entry.getKey().getSimpleName().toString().equals("name")) {
                                nameDTO = entry.getValue().getValue().toString();
                            }
                        }
                    }

                }


            } else {
                for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                    System.err.println("Processing: " + element.getSimpleName());
                    VariableElement el = (VariableElement) element;

                    for (Modifier mod : element.getModifiers() ) {
                        fields.append(mod.toString());
                    }
                    String simpleType = element.asType().toString().substring(element.asType().toString().lastIndexOf(".")+1);
                    fields.append(" ").append(simpleType).append(" ").append(element.getSimpleName().toString());
                    if(el.getConstantValue()!= null) {
                        fields.append(" = ").append(el.getConstantValue());
                    }
                    fields.append(";\n");


                }
            }

        }
        Filer filer = processingEnv.getFiler();
        try {
            JavaFileObject fileObject = filer.createSourceFile("rabah.usthb." + nameDTO);
            try (Writer writer = fileObject.openWriter()) {
                writer.write("package rabah.usthb;\n");
                writer.write("public class " + nameDTO + " {\n");
                writer.write(fields.toString());
                writer.write("}\n");

            }
        }
        catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to generate class: " + e.getMessage());
        }


        return true;
    }


}