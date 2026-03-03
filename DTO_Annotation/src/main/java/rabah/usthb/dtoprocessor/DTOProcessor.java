package rabah.usthb.dtoprocessor;

import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import java.io.IOException;
import java.io.Writer;

import java.util.*;
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

        String nameClass = "";

        List<String> nameDTOList = new LinkedList<>();
        List<StringBuilder> fieldsList = new LinkedList<>();

        for (TypeElement annotation : annotations) {


            if (annotation.getSimpleName().toString().equals("DTO")) {


                for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                    nameClass = element.getSimpleName().toString();
                    System.err.println("Processing: " + element.getSimpleName());

                    for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
                        for (Map.Entry<? extends javax.lang.model.element.ExecutableElement, ? extends javax.lang.model.element.AnnotationValue> entry : mirror.getElementValues().entrySet()) {
                            System.err.println("KEY " + entry.getKey().getSimpleName() + " VALUE : " + entry.getValue().getValue());
                            if (entry.getKey().getSimpleName().toString().equals("name")) {
                                List<? extends AnnotationValue> list = (List<? extends AnnotationValue>)entry.getValue().getValue();
                                String[] nameList = list.stream().map(v-> (String) v.getValue()).toArray(String[]::new);

                                Collections.addAll(nameDTOList,nameList);

                            }
                        }
                    }

                }


            } else {
                for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                    System.err.println("Processing: " + element.getSimpleName());
                    VariableElement el = (VariableElement) element;
                    List<String> excludedDTOList = new LinkedList<>();


                    for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
                        for (Map.Entry<? extends javax.lang.model.element.ExecutableElement, ? extends javax.lang.model.element.AnnotationValue> entry : mirror.getElementValues().entrySet()) {
                            System.err.println("KEY " + entry.getKey().getSimpleName() + " VALUE : " + entry.getValue().getValue());
                            if (entry.getKey().getSimpleName().toString().equals("excludedDTO")) {
                                List<? extends AnnotationValue> list = (List<? extends AnnotationValue>)entry.getValue().getValue();
                                String[] excludedDTO = list.stream().map(v-> (String) v.getValue()).toArray(String[]::new);
                                Collections.addAll(excludedDTOList,excludedDTO);
                            }
                        }
                    }

                    if (!excludedDTOList.isEmpty()) {
                        for (int i = 0; i < nameDTOList.size(); i++) {
                            if (!excludedDTOList.contains(nameDTOList.get(i))) {
                                try {
                                    fieldsList.get(i);
                                }
                                catch (IndexOutOfBoundsException e) {
                                    fieldsList.add(new StringBuilder());
                                }

                                fieldsList.get(i).append("\t");
                                for (Modifier mod : element.getModifiers()) {
                                    fieldsList.get(i).append(mod.toString());
                                }
                                String simpleType = element.asType().toString().substring(element.asType().toString().lastIndexOf(".") + 1);
                                fieldsList.get(i).append(" ").append(simpleType).append(" ").append(element.getSimpleName().toString());
                                if (el.getConstantValue() != null) {
                                    fieldsList.get(i).append(" = ").append(el.getConstantValue());
                                }
                                fieldsList.get(i).append(";\n");

                            }

                        }
                    }
                    else {
                        for (int i = 0; i < nameDTOList.size(); i++) {

                                try {
                                    fieldsList.get(i);
                                }
                                catch (IndexOutOfBoundsException e) {
                                    fieldsList.add(new StringBuilder());
                                }

                                fieldsList.get(i).append("\t");

                                for (Modifier mod : element.getModifiers()) {
                                    fieldsList.get(i).append(mod.toString());
                                }
                                String simpleType = element.asType().toString().substring(element.asType().toString().lastIndexOf(".") + 1);
                                fieldsList.get(i).append(" ").append(simpleType).append(" ").append(element.getSimpleName().toString());
                                if (el.getConstantValue() != null) {
                                    fieldsList.get(i).append(" = ").append(el.getConstantValue());
                                }
                                fieldsList.get(i).append(";\n");

                            }


                    }





                }
            }

        }
        Filer filer = processingEnv.getFiler();
        for (int i = 0; i<nameDTOList.size();i++) {
            try {
                JavaFileObject fileObject = filer.createSourceFile("rabah.usthb." + nameDTOList.get(i)+nameClass+"DTO");
                try (Writer writer = fileObject.openWriter()) {
                    writer.write("package rabah.usthb;\n");
                    writer.write("public class " + nameDTOList.get(i)+nameClass+"DTO {\n");
                    writer.write(fieldsList.get(i).toString());
                    writer.write("}\n");

                }
            }
            catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to generate class: " + e.getMessage());
            }
        }


        return true;
    }


}