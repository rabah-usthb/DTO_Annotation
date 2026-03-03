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
@javax.annotation.processing.SupportedAnnotationTypes("rabah.usthb.dtoprocessor.DTO")
@javax.annotation.processing.SupportedSourceVersion(javax.lang.model.SourceVersion.RELEASE_17)
public class DTOProcessor extends AbstractProcessor {

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        for (TypeElement annotation : annotations) {



            if (annotation.getSimpleName().toString().equals("DTO")) {
                String nameDTO = "" ;

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

                    Filer filer = processingEnv.getFiler();
                    try {
                        JavaFileObject fileObject = filer.createSourceFile("rabah.usthb."+nameDTO);
                        try (Writer writer = fileObject.openWriter()) {
                            writer.write("package rabah.usthb;\n");
                            writer.write("public class "+nameDTO+" {\n");
                            writer.write("    // Generated contentsssssssssss\n");
                            writer.write("}\n");
                        }
                    } catch (IOException e) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to generate class: " + e.getMessage());
                    }

            }
        }


        return true;
    }
}