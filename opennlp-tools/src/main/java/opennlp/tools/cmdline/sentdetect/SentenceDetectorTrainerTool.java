/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package opennlp.tools.cmdline.sentdetect;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import opennlp.model.TrainUtil;
import opennlp.tools.cmdline.AbstractTrainerTool;
import opennlp.tools.cmdline.CmdLineUtil;
import opennlp.tools.cmdline.TerminateToolException;
import opennlp.tools.cmdline.params.TrainingToolParams;
import opennlp.tools.cmdline.sentdetect.SentenceDetectorTrainerTool.TrainerToolParams;
import opennlp.tools.dictionary.Dictionary;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.sentdetect.SentenceSample;
import opennlp.tools.util.model.ModelUtil;

public final class SentenceDetectorTrainerTool
    extends AbstractTrainerTool<SentenceSample, TrainerToolParams> {
  
  interface TrainerToolParams extends TrainingParams, TrainingToolParams {
  }

  public SentenceDetectorTrainerTool() {
    super(SentenceSample.class, TrainerToolParams.class);
  }

  public String getShortDescription() {
    return "trainer for the learnable sentence detector";
  }
  
  static Dictionary loadDict(File f) throws IOException {
    Dictionary dict = null;
    if (f != null) {
      CmdLineUtil.checkInputFile("abb dict", f);
      dict = new Dictionary(new FileInputStream(f));
    }
    return dict;
  }
  
  public void run(String format, String[] args) {
    super.run(format, args);

    mlParams = CmdLineUtil.loadTrainingParameters(params.getParams(), false);

    if (mlParams != null) {
      if (TrainUtil.isSequenceTraining(mlParams.getSettings())) {
        throw new TerminateToolException(1, "Sequence training is not supported!");
      }
    }

    if(mlParams == null) {
      mlParams = ModelUtil.createTrainingParameters(params.getIterations(), params.getCutoff());
    }

    File modelOutFile = params.getModel();
    CmdLineUtil.checkOutputFile("sentence detector model", modelOutFile);

    SentenceModel model;
    try {
      Dictionary dict = loadDict(params.getAbbDict());
      model = SentenceDetectorME.train(factory.getLang(), sampleStream, true, dict, mlParams);
    } catch (IOException e) {
      throw new TerminateToolException(-1, "IO error while reading training data or indexing data: " + e.getMessage());
    }
    finally {
      try {
        sampleStream.close();
      } catch (IOException e) {
        // sorry that this can fail
      }
    }
    
    CmdLineUtil.writeModel("sentence detector", modelOutFile, model);
  }
}
