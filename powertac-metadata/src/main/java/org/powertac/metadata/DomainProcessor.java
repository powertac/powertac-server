/*
 * Copyright (c) 2019 by John Collins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.powertac.metadata;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.FilerException;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.SourceVersion;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import org.powertac.common.state.Domain;

//import org.powertac.common.state.Domain;

/**
 * @author John Collins
 *
 */
@SupportedAnnotationTypes("org.powertac.common.state.Domain")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class DomainProcessor extends AbstractProcessor
{

  public DomainProcessor ()
  {
    super();
  }

  /* (non-Javadoc)
   * @see javax.annotation.processing.AbstractProcessor#process(java.util.Set, javax.annotation.processing.RoundEnvironment)
   */
  @Override
  public boolean process (Set<? extends TypeElement> annotations,
                          RoundEnvironment roundEnv)
  {
    // start by opening a file for metadata recording
    PrintWriter out;
    try {
      FileObject f =
              processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT,
                                                      "",
                                                      "metadata/domain.schema");
      out = new PrintWriter(f.openWriter(), true);
    }
    catch (FilerException e) {
      // happens in normal processing
      return true;
    }
    catch (IOException e) {
      e.printStackTrace();
      return true;
    }

    Map<String, String> opts = processingEnv.getOptions();
    //System.out.println("Env options: " + opts.size());
    //for (String key : opts.keySet())
    //  System.out.println("Key " + key);
    String artifactVersion = opts.get("core.versionId");
    if (null == artifactVersion) {
      System.out.println("Could not retrieve versionId");
    }
    out.format("Domain-schema-version:%s\n", artifactVersion);
    if (annotations.size() != 1) {
      System.out.println("Should be only one annotation, saw " + annotations.size());
    }
    for (TypeElement annotation : annotations) {
      //System.out.println("annotation name: " + annotation.getQualifiedName());
      for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
        TypeMirror tm = element.asType();
        if (tm.getKind() == TypeKind.DECLARED) {
          Domain domain = element.getAnnotation(Domain.class);
          if (domain.fields().length > 0) {
            out.format("%s:", tm.toString());
            String delim = "";
            for (String field: domain.fields()) {
              out.format("%s%s", delim, field);
              delim = ",";
            }
            out.format("\n");
          }
        }
      }
    }
    out.format("schema.end\n");
    out.close();
    return true;
  }

}
