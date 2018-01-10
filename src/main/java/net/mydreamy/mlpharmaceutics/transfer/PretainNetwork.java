package net.mydreamy.mlpharmaceutics.transfer;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;
import org.datavec.api.util.ClassPathResource;
import org.deeplearning4j.api.storage.StatsStorage;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.datasets.datavec.RecordReaderMultiDataSetIterator;
import org.deeplearning4j.earlystopping.EarlyStoppingConfiguration;
import org.deeplearning4j.earlystopping.EarlyStoppingResult;
import org.deeplearning4j.earlystopping.saver.LocalFileModelSaver;
import org.deeplearning4j.earlystopping.scorecalc.DataSetLossCalculator;
import org.deeplearning4j.earlystopping.termination.EpochTerminationCondition;
import org.deeplearning4j.earlystopping.termination.MaxEpochsTerminationCondition;
import org.deeplearning4j.earlystopping.trainer.EarlyStoppingTrainer;
import org.deeplearning4j.eval.BaseEvaluation;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.eval.EvaluationBinary;
import org.deeplearning4j.eval.ROC;
import org.deeplearning4j.eval.RegressionEvaluation;
import org.deeplearning4j.nn.api.Layer;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.ComputationGraphConfiguration;
import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.LearningRatePolicy;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.WorkspaceMode;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.transferlearning.FineTuneConfiguration;
import org.deeplearning4j.nn.transferlearning.TransferLearning;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.jita.conf.CudaEnvironment;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.impl.accum.MatchCondition;
import org.nd4j.linalg.api.ops.impl.indexaccum.IAMax;
import org.nd4j.linalg.api.ops.impl.transforms.Abs;
import org.nd4j.linalg.api.ops.impl.transforms.And;
import org.nd4j.linalg.api.ops.impl.transforms.ReplaceNans;
import org.nd4j.linalg.api.ops.impl.transforms.Xor;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.MultiDataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.iterator.MultiDataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.MultiNormalizerMinMaxScaler;
import org.nd4j.linalg.dataset.api.preprocessor.MultiNormalizerStandardize;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.BooleanIndexing;
import org.nd4j.linalg.indexing.INDArrayIndex;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.linalg.indexing.SpecifiedIndex;
import org.nd4j.linalg.indexing.conditions.Conditions;
import org.nd4j.linalg.indexing.conditions.IsNaN;
import org.nd4j.linalg.indexing.conditions.Not;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.nd4j.linalg.ops.transforms.Transforms;
import org.nd4j.linalg.util.ArrayUtil;

import javafx.application.Application;


/**
 * 
 * @author Yilong
 *
 * MultTask with MaskArray not transfer learning
 * 
 * 
 *
 */
public class PretainNetwork {
	
	static int epoch = 1;
	static int trainsetsize = 432803;
	static int batchSize = 200;
	static int totalNumberofBatch = trainsetsize / batchSize;
	static double learningrate = 0.03;
	static double lambd = 0.01;
	static double beta1 = 0.5;
	static double beta2 = 0.999;
	
	
	static double activitynumber = 0;
	static double activitypredictioncorectnessnumber = 0;
	static double existtargetnumber = 0;

	public static void main(String[] args) {
		
		if (args.length == 3) {
			epoch = Integer.valueOf(args[2]);
		}
//		CudaEnvironment.getInstance().getConfiguration().
//		setMaximumDeviceCacheableLength(1024 * 1024 * 1024L).
//		setMaximumDeviceCache((long) (0.5 * 8 * 1024L * 1024L * 1024L)).
//		setMaximumHostCacheableLength(1024 * 1024 * 1024L).
//		setMaximumHostCache((long) (0.5 * 8 * 1024 * 1024 * 1024L));
//		
//		CudaEnvironment.getInstance().getConfiguration().allowMultiGPU(true);
		
		// Disable GC
		Nd4j.getMemoryManager().togglePeriodicGc(false);
		
		//data read
		int numLinesToSkip = 0;
		
		//ADME reader
		RecordReader ADME = new CSVRecordReader(numLinesToSkip,',');
		
		try {
			ADME.initialize(new FileSplit(new File(args[0])));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		MultiDataSetIterator ADMEiter = new RecordReaderMultiDataSetIterator.Builder(batchSize)
			
		        .addReader("adme", ADME)
		        .addInput("adme", 0, 1023)  //1024 finger prints
		        .addOutput("adme", 1024, 1024+157-1) //157 tasks
//		        .addOutput("adme", 1024, 1024+10-1) //157 tasks

		        .build();
		
		

		//TestReader
		RecordReader ADMEdev = new CSVRecordReader(numLinesToSkip, ',');
				
		try {
			ADMEdev.initialize(new FileSplit(new File(args[1])));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
						
		MultiDataSetIterator ADMEDeviter = new RecordReaderMultiDataSetIterator.Builder(batchSize)
					
				     	.addReader("adme", ADMEdev)
				        .addInput("adme", 0, 1023)  //1024 finger prints
				        .addOutput("adme", 1024, 1024+157-1) //157 tasks
//				        .addOutput("adme", 1024, 1024) //157 tasks

				        .build();		
		
				
		//final network
		ComputationGraphConfiguration conf = new NeuralNetConfiguration.Builder() //create global builder 
				
				//workspaceMode
				.trainingWorkspaceMode(WorkspaceMode.SINGLE)
				.inferenceWorkspaceMode(WorkspaceMode.SINGLE)
				
				//flowing method set attribute then return this object
				.seed(123456)
	            .learningRate(learningrate)
//	            .learningRateDecayPolicy(LearningRatePolicy.Inverse)
//	            .lrPolicyDecayRate(0.001).lrPolicyPower(2)
	            .updater(Updater.ADAM)           
                .weightInit(WeightInit.XAVIER)
//                .regularization(true)
//                .l1(lambd)
		        .graphBuilder()  //create GraphBuilder with global builder 
				       
		        
		        .addInputs("INPUT") //set input layers
		        .setOutputs("TASKS") //set output layers
		        
		        //nIn nOut at DenseLayer.Builder(), activation in BaseLayer.Builder() << abstract Layer.Builder() (dropout here)
		        .addLayer("L1", new DenseLayer.Builder().activation(Activation.TANH).nIn(1024).nOut(1000).build(), "INPUT")
		        .addLayer("L2", new DenseLayer.Builder().activation(Activation.TANH).nIn(1000).nOut(900).build(), "L1")
		        .addLayer("L3", new DenseLayer.Builder().activation(Activation.TANH).nIn(900).nOut(800).build(), "L2")
		        .addLayer("L4", new DenseLayer.Builder().activation(Activation.TANH).nIn(800).nOut(700).build(), "L3")
		        .addLayer("M1", new DenseLayer.Builder().activation(Activation.TANH).nIn(700).nOut(600).build(), "L4")
		        .addLayer("M2", new DenseLayer.Builder().activation(Activation.TANH).nIn(600).nOut(500).build(), "M1")
		        .addLayer("M3", new DenseLayer.Builder().activation(Activation.TANH).nIn(500).nOut(400).build(), "M2")
		        .addLayer("M4", new DenseLayer.Builder().activation(Activation.TANH).nIn(400).nOut(300).build(), "M3")
		        .addLayer("M5", new DenseLayer.Builder().activation(Activation.TANH).nIn(300).nOut(200).build(), "M4")	       
		        .addLayer("FEATURE", new DenseLayer.Builder().activation(Activation.TANH).nIn(200).nOut(100).build(), "M5")
		        .addLayer("HIDDEN", new DenseLayer.Builder().activation(Activation.TANH).nIn(100).nOut(1000).build(), "FEATURE")


		        .addLayer("TASKS", new OutputLayer.Builder().activation(Activation.SIGMOID)
		                .lossFunction(new WeightedL2Loss(1))
		                .nIn(1000).nOut(157).build(), "HIDDEN")
		      

		        .backprop(true)
		        .build();  //use set all parameters and global configuration to create a object of ComputationGraphConfiguration
		
		ComputationGraph net = new ComputationGraph(conf);

		
		net.init();
		
		

		
		System.out.println("-------------------- training ADME ----------------------- ");
		
		System.out.println("Epoches:" + epoch);
		
		for (int i = 0; i < epoch; i++) {
		
			int numberOfBatchSize = 1;
			MultiDataSet data = null;
			double epochTime = 0;
			double subEpochTime = 0;
			double subloadingtime = 0;
			double submaskingtime = 0;
			double subacc = 0;
			
			while (ADMEiter.hasNext()) {
				
				
				//data loading
				long substart = System.currentTimeMillis();
				data = ADMEiter.next();
				double loadingtime =  ((double) System.currentTimeMillis() - substart);
				subloadingtime+=loadingtime;
 				
				
				//apply label mask
				substart = System.currentTimeMillis();
				INDArray[] masks = computeOutPutMaskBinaray(data);
				data.setLabelsMaskArray(masks);	
				double maskingtime =  ((double) System.currentTimeMillis() - substart);
				submaskingtime+=maskingtime;
				
				//fit data
				substart = System.currentTimeMillis();
//				System.out.println("prediction" + net.output(data.getFeatures()[0])[0]);
				net.fit(data);
//				System.out.println("batch error:" + net.score() + "\n");
//				net.fit(data);	
//				System.out.println("batch error:" + net.score() + "\n");
	
				double traintime =  ((double) System.currentTimeMillis() - substart);
				epochTime += loadingtime+maskingtime+traintime;
				subEpochTime += loadingtime+maskingtime+traintime;
					
				
//				System.out.println("features:" + data.getFeatures()[0]);
//				System.out.println("masks:" + data.getLabelsMaskArrays()[0]);
//
//				System.out.println("labels:" + data.getLabels()[0]);
//
//				System.out.println("predictions:" + net.output(data.getFeatures()[0])[0]);
//				
//				System.out.println("\n\n");
				
//				System.out.println(computeAUC(data.getLabels()[0], net.output(data.getFeatures()[0])[0], masks[0]));
				
				
				float batchacc = computeBatchPostiveAccuracy(data.getLabels()[0], net.output(data.getFeatures()[0])[0], masks[0], 0.5);
//				System.out.println("batchacc" + batchacc);
				subacc+=batchacc;
				
				if (numberOfBatchSize % 50 == 0) {
					System.out.println("epoch:" + i + ", batch number:" + numberOfBatchSize + "/" + totalNumberofBatch + ", 50 loadding time:" + subloadingtime + " ms, masking time:"+ 
								submaskingtime + " ms, training time:" + String.format("%.2f", subEpochTime) + " ms" + ", time elaspe:" +  
								String.format("%.2f", epochTime/1000F) + " s" + ", exist number:" +  existtargetnumber + ", activity number:" + activitynumber +
								" activity rate: " + String.format("%.2f", (activitynumber / existtargetnumber)*100)  + 
								"%, correct activity number: " + activitypredictioncorectnessnumber + 
								", 50 batch acc:" + subacc/50L*100 + " %" + ", error: " + net.score());
					subEpochTime = 0;
					subloadingtime = 0;
					submaskingtime = 0;
					subacc = 0;
					activitynumber = 0;
					activitypredictioncorectnessnumber = 0;
					existtargetnumber = 0;
				}
				
				numberOfBatchSize++;

			}
			
			ADMEiter.reset();
			
			System.out.println("Epoch Time: " + epochTime/(60*1000F) + "min");
			epochTime = 0;
			subEpochTime = 0;
			
			//evalute every 10 epochs
//			if (i % 1 == 0) {				
//				System.out.println("-------------------- epoch " + i + "----------------------- ");
//				System.out.println("-------------------- tranning set ----------------------- ");
//				testing(net, ADMEiter, MSEs, true, R2s, false, accurecyMAEs, true, false);
//	
//				System.out.println("-------------------- validation set ----------------------- ");
//				testing(net, ADMEDeviter, MSEDevs, true, R2Devs, false, accurecyMAEDevs, true, false);
//		
//
//			}
		}		
	
		//Net Configuration Summary
		System.out.println(net.summary());
		System.out.println("batchsize:" + batchSize);
		System.out.println("learning rate:" + learningrate);
		System.out.println("total epoch: " + epoch);
		System.out.println("beta1: " + beta1);
		System.out.println("beta2: " + beta2);
		System.out.println("lambda :" + lambd); 
		
		
		
		System.out.println("-------------------- final testing ADME ----------------------- ");
		System.out.println("-------------------- tranning set ----------------------- ");
//		testing(net, ADMEiter, MSEs, true, R2s, false, accurecyMAEs, true, false);
		
		System.out.println("-------------------- accuracy only activity  ----------------------- ");
		activitynumber = 0;
		activitypredictioncorectnessnumber = 0;
		System.out.println("test accuracy only 1:" +  MultiBinaryPostiveAccuracy(net, ADMEiter)*100 + "%");
		System.out.println("activitynumber:" + activitynumber);
		System.out.println("activitypredictioncorectnessnumber: " + activitypredictioncorectnessnumber);
		
		System.out.println("-------------------- binary accuracy  ----------------------- ");	
		System.out.println("test accuracy 0 and 1:" +  MultiBinaryAccuracy(net, ADMEiter)*100 + "%");		
		
		System.out.println("-------------------- AUC  ----------------------- ");	
		System.out.println("test accuracy AUC:" +  MultiAUC(net, ADMEiter)*100 + "%");		


		System.out.println("-------------------- validation set ----------------------- ");
//		testing(net, ADMEDeviter, MSEDevs, true, R2Devs, false, accurecyMAEDevs, true, false);
		
		System.out.println("-------------------- accuracy only activity  ----------------------- ");
		activitynumber = 0;
		activitypredictioncorectnessnumber = 0;
		System.out.println("test accuracy:" +  MultiBinaryPostiveAccuracy(net, ADMEDeviter)*100 + "%");
		System.out.println("activitynumber: " + activitynumber);
		System.out.println("activitypredictioncorectnessnumber: " + activitypredictioncorectnessnumber);
		
		System.out.println("-------------------- binary accuracy  ----------------------- ");	
		System.out.println("test accuracy:" +  MultiBinaryAccuracy(net, ADMEDeviter)*100 + "%");		
		
		System.out.println("-------------------- AUC  ----------------------- ");	
		System.out.println("test accuracy AUC:" +  MultiAUC(net, ADMEDeviter)*100 + "%");	
		
	}
	
	public static double computeAUC(INDArray lables, INDArray prediction, INDArray masks) {
		
		ROC roc = new ROC();
		
		double totalauc = 0;
		
		int col = lables.columns();
		int row = lables.rows();
		
		for (int i = 0; i < col; i++) {
			
			List<Double> labelvector = new ArrayList<Double>();
			List<Double> predictionvector = new ArrayList<Double>();

			
			for (int j = 0; j < row; j++) {
				
				if (masks.getDouble(j, i) == 1) {
					
					labelvector.add(lables.getDouble(j, i));
					predictionvector.add(prediction.getDouble(j, i));
									
				}
				
			}
			
			if (labelvector.size() > 0) {
				double[] labelarray = ArrayUtils.toPrimitive(labelvector.toArray(new Double[labelvector.size()]));
				double[] predictarray = ArrayUtils.toPrimitive(labelvector.toArray(new Double[labelvector.size()]));
				
				roc.eval(Nd4j.create(labelarray).transpose(), Nd4j.create(predictarray).transpose());
				totalauc+=roc.calculateAUC();
			}
			
		}
		
		return totalauc / (float) col;
		
	}
	
	public static float MultiAUC(ComputationGraph net, MultiDataSetIterator iter) {
		
		double starttime = System.currentTimeMillis();
		
		iter.reset();
		
		float i = 0;
		float score = 0;
		
		while (iter.hasNext()) {
			
			MultiDataSet data = iter.next();
			
			INDArray[] masks = computeOutPutMaskBinaray(data);
			
			score += computeAUC(data.getLabels()[0], net.output(data.getFeatures()[0])[0], masks[0]);
				
			i++;
			
			if (i % 100 == 0) 
				System.out.println("test on sub batch: " + i + "/" + totalNumberofBatch);
		}
		
		iter.reset();
		
		System.out.println("Test time elasped: " + (System.currentTimeMillis() - starttime) / 1000F + "s");
		
		return score/i;
		
	}
	
	public static float MultiBinaryAccuracy(ComputationGraph net, MultiDataSetIterator iter) {
		
		double starttime = System.currentTimeMillis();
		
		iter.reset();
		
		float i = 0;
		float score = 0;
		
		while (iter.hasNext()) {
			
			MultiDataSet data = iter.next();
			
			INDArray[] masks = computeOutPutMaskBinaray(data);
			
			score += computeBatchAccuracy(data.getLabels()[0], net.output(data.getFeatures()[0])[0], masks[0], 0.5);
				
			i++;
			
			if (i % 100 == 0) 
				System.out.println("test on sub batch: " + i + "/" + totalNumberofBatch);
		}
		
		iter.reset();
		
		System.out.println("Test time elasped: " + (System.currentTimeMillis() - starttime) / 1000F + "s");
		
		return score/i;
		
	}
	
	public static float MultiBinaryPostiveAccuracy(ComputationGraph net, MultiDataSetIterator iter) {
		
		double starttime = System.currentTimeMillis();
		
		iter.reset();
		
		float i = 0;
		float score = 0;
		
		while (iter.hasNext()) {
			
			MultiDataSet data = iter.next();
			
			INDArray[] masks = computeOutPutMaskBinaray(data);
			
			float currentscore = computeBatchPostiveAccuracy(data.getLabels(0), net.outputSingle(data.getFeatures(0)), masks[0], 0.5);
					
			if (score != -1) {
				score += currentscore;
				i++;
			}				
			
			if (i % 100 == 0) 
				System.out.println("test on sub batch: " + i + "/" + totalNumberofBatch);
		}
		
		iter.reset();
		
		System.out.println("Test time elasped: " + (System.currentTimeMillis() - starttime) / 1000F + "s");
		
		return score/i;
		
	}
	
	public static float computeBatchAccuracy(INDArray lablesTest, INDArray PredictionTest, INDArray mask, double therdsold) {
		

		BooleanIndexing.replaceWhere(PredictionTest, 1,  Conditions.greaterThanOrEqual(therdsold));
		BooleanIndexing.replaceWhere(PredictionTest, 0,  Conditions.lessThan(therdsold));
		
//		System.out.println("PredictionTest" + PredictionTest);
//		System.out.println("lablesTest" + lablesTest);
		
		
		int batchrows = lablesTest.rows();
		int batchcolumns = lablesTest.columns();
		float vaildlength = mask.sumNumber().intValue();
//		System.out.println("validlength" + vaildlength);
		float correctnum = 0;
						
		for (int m = 0; m < batchrows; m++) {
			for (int n = 0; n < batchcolumns; n++) {
				
				if (mask.getDouble(m, n) == 1) {
			
					if (PredictionTest.getDouble(m, n) == lablesTest.getDouble(m, n)) {
						correctnum++;
//						System.out.println("mask1");
					}
				}
				
			}
		}
		
		
//		System.out.println("correctnum" + correctnum);

		
		float batchcorrectness =  correctnum / vaildlength;
		
		return batchcorrectness;
	}
	
	public static float computeBatchPostiveAccuracy(INDArray lablesTest, INDArray PredictionTest, INDArray mask, double therdsold) {
		
//		System.out.println("label:" + lablesTest);
//		System.out.println("predict:" + PredictionTest);
		
		BooleanIndexing.replaceWhere(PredictionTest, 1,  Conditions.greaterThanOrEqual(therdsold));
		BooleanIndexing.replaceWhere(PredictionTest, 0,  Conditions.lessThan(therdsold));
		
		
		int batchrows = lablesTest.rows();
		int batchcolumns = lablesTest.columns();
		float vaildlength = 0;
		float correctnum = 0;
		float existednum = 0;
						
		for (int m = 0; m < batchrows; m++) {
			for (int n = 0; n < batchcolumns; n++) {
					
				if (mask.getDouble(m, n) == 1) {
					existednum++;
					if (lablesTest.getDouble(m, n) == 1) {				
						vaildlength++;
						if (PredictionTest.getDouble(m, n) == 1) {
							correctnum++;
						}
					}
				}
				
			}
		}
		
//		System.out.println("vaild lenght:" + vaildlength);
//		System.out.println("correct num:" + correctnum);
		
		if (vaildlength == 0) {
			
			return -1;
			
		} else {
			
			existtargetnumber += existednum;
			activitynumber += vaildlength;
			activitypredictioncorectnessnumber += correctnum;
			
			float batchcorrectness =  correctnum / vaildlength;
			return batchcorrectness;
		}
	}
	
	public static void testing(ComputationGraph net, MultiDataSetIterator ADMEiter, List<double []> mses, boolean printMSE, List<double []> R2s, boolean printR2, List<double []> MAEarruaccys, boolean printMAEarruaccy, boolean printPerdiction) {
		
		ADMEiter.reset();
		
		MultiDataSet data = null;
		
		int numOfBatch = 0;
		double sumR2[] = {0,0,0,0};
		double sumMAE[] = {0,0,0,0};
		double sumaccurecyMAE[] = {0,0,0,0};
		
		while (ADMEiter.hasNext()) {
			
			data = ADMEiter.next();
			
			int numlabels = data.numLabelsArrays();
			
			//compute mask
			INDArray[] masks = computeOutPutMask(data);
	
			//apply label mask
			data.setLabelsMaskArray(masks);
	
			INDArray[] labels = data.getLabels();
			INDArray[] predictions = net.output(data.getFeatures(0));
			
	
			
//			Application.launch(LineChartApp.class, null);

		
			
			for (int i = 0; i < numlabels; i++) {
				
				if (printPerdiction) {
					
					System.out.println("column: " + i);
					 
					int length = labels[i].length();
					
					System.out.println("label: " + i + " ");
					for (int j = 0; j < length; j++) {

						if (labels[i].getDouble(j) != -1) {
							System.out.print("number " + j + ": ");
							System.out.print(labels[i].getDouble(j) + " ");
							System.out.println(predictions[i].getDouble(j));
						}
							

						
					}
				}
//				System.out.println("mask: " + i + " " + masks[i].toString());
//				System.out.println("");
				
				sumR2[i] += AccuracyRSquare(labels[i], predictions[i], masks[i]);
				sumMAE[i] += MAE(labels[i], predictions[i], masks[i]);
				sumaccurecyMAE[i] += AccuracyMAE(labels[i], predictions[i], masks[i], 0.1);
				
			}

			numOfBatch++;
			
		}
		
		//compuate R2 on all batches
		
		sumR2[0]/=numOfBatch;
		sumR2[1]/=numOfBatch;
		sumR2[2]/=numOfBatch;
		sumR2[3]/=numOfBatch;
		
		R2s.add(sumR2);
		
		if (printR2) {
			
			System.out.println("================== R squared ==================");
			System.out.println("R2[0]" +  String.format("%.4f", sumR2[0]/numOfBatch));
			System.out.println("R2[1]" +  String.format("%.4f", sumR2[1]/numOfBatch));
			System.out.println("R2[2]" +  String.format("%.4f", sumR2[2]/numOfBatch));
			System.out.println("R2[3]" +  String.format("%.4f", sumR2[3]/numOfBatch));
		}
		
		
		//compute MAE on all batches
		
		sumMAE[0]/=numOfBatch;
		sumMAE[1]/=numOfBatch;
		sumMAE[2]/=numOfBatch;
		sumMAE[3]/=numOfBatch;
		
		mses.add(sumMAE);

		if (printMSE) {
			System.out.println("================== MAE ==================");
			System.out.println(String.format("%.4f", sumMAE[0]));
			System.out.println(String.format("%.4f", sumMAE[1]));
			System.out.println(String.format("%.4f", sumMAE[2]));
			System.out.println(String.format("%.4f", sumMAE[3]));
		}
		
		//compute accuracyMAE on all batches
		
		sumaccurecyMAE[0]/=numOfBatch;
		sumaccurecyMAE[1]/=numOfBatch;
		sumaccurecyMAE[2]/=numOfBatch;
		sumaccurecyMAE[3]/=numOfBatch;
		
		MAEarruaccys.add(sumaccurecyMAE);

		if (printMAEarruaccy) {
			System.out.println("================== MAEarruaccys ==================");
			System.out.println(String.format("%.4f", sumaccurecyMAE[0]));
			System.out.println(String.format("%.4f", sumaccurecyMAE[1]));
			System.out.println(String.format("%.4f", sumaccurecyMAE[2]));
			System.out.println(String.format("%.4f", sumaccurecyMAE[3]));
		}
		
		
		ADMEiter.reset();
		
		
		
	}
	

	public static void printAllCost(List<double []> errors, List<double []> errorDevs, List<double []> errorsT) {
		
		System.out.println("-------------------- training set error ----------------------- ");
		for (double[] e : errors) {
			System.out.print(e[0] + " ");
		}
		
		System.out.println("\n");
		
		for (double[] e : errors) {
			System.out.print(e[1] + " ");
		}
		
		System.out.println("\n");
		
		for (double[] e : errors) {
			System.out.print(e[2] + " ");
		}
		
		System.out.println("\n");
		
		for (double[] e : errors) {
			System.out.print(e[3] + " ");
		}

		System.out.println("");
		System.out.println("-------------------- validation set error ----------------------- ");

		for (double[] e : errorDevs) {
			System.out.print(e[0] + " ");
		}
		
		System.out.println("\n");
		
		for (double[] e : errorDevs) {
			System.out.print(e[1] + " ");
		}
		
		System.out.println("\n");
		
		for (double[] e : errorDevs) {
			System.out.print(e[2] + " ");
		}
		
		System.out.println("\n");
		
		for (double[] e : errorDevs) {
			System.out.print(e[3] + " ");
		}		
		
		System.out.println("");
		System.out.println("-------------------- testing set error ----------------------- ");

		for (double[] e : errorsT) {
			System.out.print(e[0] + " ");
		}
		
		System.out.println("\n");
		
		for (double[] e : errorsT) {
			System.out.print(e[1] + " ");
		}
		
		System.out.println("\n");
		
		for (double[] e : errorsT) {
			System.out.print(e[2] + " ");
		}
		
		System.out.println("\n");
		
		for (double[] e : errorsT) {
			System.out.print(e[3] + " ");
		}
		
	}
	
	public static double AccuracyRSquare(INDArray lablesTest, INDArray PredictionTest, INDArray mask) {
		
		int vaildlength = mask.sumNumber().intValue();
		
		//apply mask
		lablesTest.muli(mask);
		PredictionTest.muli(mask);
		
		if (vaildlength != 0) {
		
	        Double labelmean = lablesTest.sum(0).getDouble(0) / vaildlength;
	        
	        Double SSS = Transforms.pow(lablesTest.sub(PredictionTest), 2).sum(0).getDouble(0);
	        
	        Double SST = Transforms.pow(lablesTest.sub(labelmean), 2).sum(0).getDouble(0);
	        Double SSG = Transforms.pow(PredictionTest.sub(labelmean), 2).sum(0).getDouble(0);
	        
//	        Double R = SSG/SST;
	        Double R =  1 - (SSS/SST);
	        
	//        log.info("label mean: " + labelmean);
	        
	//        log.info("SSE: " + SSE);
	//        log.info("SST: " + SST);
	
//	        System.out.println("R square: " + (1 - (SSS/SST)));
//	        System.out.println("Sub R square: " + R);
	        
	        return R;
        
		} else {
			
			return 0;
			
		}
	}
	
	
	public static double MSE(INDArray lablesTest, INDArray PredictionTest, INDArray mask) {
		
		int vaildlength = mask.sumNumber().intValue();
		
//		System.out.println("vaild number" + vaildlength);
		
		//apply mask
		lablesTest.muli(mask);
		PredictionTest.muli(mask);
		
		BooleanIndexing.replaceWhere(lablesTest, 0,  Conditions.isNan());
		
//		System.out.println("mask" + mask);
//		System.out.println("lablesTest" + lablesTest);
//		System.out.println("PredictionTest" + PredictionTest);

		
		
		if (vaildlength != 0) {

	        Double mse = Transforms.pow(lablesTest.sub(PredictionTest), 2).sum(0).getDouble(0) / vaildlength;

//	        System.out.println("Sub MAE square: " + mae);
	        
	        return mse;
        
		} else {
			
			return 0;
			
		}
	}
	
	public static double MAE(INDArray lablesTest, INDArray PredictionTest, INDArray mask) {
		
		int vaildlength = mask.sumNumber().intValue();
		
		//apply mask
		lablesTest.muli(mask);
		PredictionTest.muli(mask);
		
		if (vaildlength != 0) {

	        Double mae = Transforms.abs(lablesTest.sub(PredictionTest)).sum(0).getDouble(0) / vaildlength;

//	        System.out.println("Sub MAE square: " + mae);
	        
	        return mae;
        
		} else {
			
			return 0;
			
		}
	}
	
	public static double AccuracyMAE(INDArray lablesTest, INDArray PredictionTest, INDArray mask, double therdsold) {
		
        INDArray absErrorMatrix = Transforms.abs(lablesTest.sub(PredictionTest));
        int size = absErrorMatrix.size(0);
        double validasize = 0;
        double correct = 0;
        
        for (int i = 0; i < size; i++)
        {
        	if (mask.getDouble(i) == 1) {
        		
        		validasize++;
        		
	        	if (absErrorMatrix.getDouble(i) <= therdsold) {
	        		correct++;
	        	}
        	}	
        }
        
        return correct/validasize;
      // log.info(allAE.toString());
      //  log.info("AccuracyMAE  <= " + therdsold*100 + "%: " + String.format("%.4f", correct/size));
	}

	
	//compute mask array for multi-labels, change NaN of origin data as -1
	public static INDArray[] computeOutPutMask(MultiDataSet data) {

		INDArray[] lables = data.getLabels();
		
		//Create Mask Array
		INDArray[] outputmask = new INDArray[lables.length];
		
		for (int j = 0; j < lables.length; j++) {

			outputmask[j] = lables[j].dup();
		
			//assign not Nan as 1
			outputmask[j].divi(outputmask[j]);		
			
//			BooleanIndexing.replaceWhere(outputmask[j], 1,  Conditions.greaterThan(-100000));

			//assign NaN as 0
			Nd4j.clearNans(outputmask[j]);			
//			BooleanIndexing.replaceWhere(outputmask[j], 0,  Conditions.isNan());
			
			//avoiding NaN bug when applying mask array
//			BooleanIndexing.replaceWhere(lables[j], -1,  Conditions.isNan());
			Nd4j.getExecutioner().exec(new ReplaceNans(lables[j], -1));
			
		}
		
		return outputmask;
		
	}
	
	//compute mask array for multi-labels, change NaN of origin data as -1
	public static INDArray[] computeOutPutMaskBinaray(MultiDataSet data) {

		INDArray[] lables = data.getLabels();
		
		//Create Mask Array
		INDArray[] outputmask = new INDArray[lables.length];
		
		for (int j = 0; j < lables.length; j++) {

			outputmask[j] = lables[j].dup();
		
			//assign NaN as 0
			
			BooleanIndexing.replaceWhere(outputmask[j], -1,  Conditions.isNan());
			BooleanIndexing.replaceWhere(outputmask[j], 1,  Conditions.notEquals(-1));
			BooleanIndexing.replaceWhere(outputmask[j], 0,  Conditions.equals(-1));

			
		}

		//avoiding NaN bug when applying mask array
		Nd4j.getExecutioner().exec(new ReplaceNans(lables[0], -1));
		
		return outputmask;
		
	}
	
	private List<double []> MSEs;
	private List<double []> MSEDevs;
	private List<double []> MSETs;
	
	private List<double []> accurecyMAEs;
	private List<double []> accurecyMAEDevs;
	private List<double []> accurecyMAETs;

	
	
	
	public List<double[]> getAccurecyMAETs() {
		return accurecyMAETs;
	}

	public void setAccurecyMAETs(List<double[]> accurecyMAETs) {
		this.accurecyMAETs = accurecyMAETs;
	}

	public List<double[]> getAccurecyMAEs() {
		return accurecyMAEs;
	}

	public void setAccurecyMAEs(List<double[]> accurecyMAEs) {
		this.accurecyMAEs = accurecyMAEs;
	}

	public List<double[]> getAccurecyMAEDevs() {
		return accurecyMAEDevs;
	}

	public void setAccurecyMAEDevs(List<double[]> accurecyMAEDevs) {
		this.accurecyMAEDevs = accurecyMAEDevs;
	}

	public List<double[]> getMSEs() {
		return MSEs;
	}

	public void setMSEs(List<double[]> mSEs) {
		MSEs = mSEs;
	}

	public List<double[]> getMSEDevs() {
		return MSEDevs;
	}

	public void setMSEDevs(List<double[]> mSEDevs) {
		MSEDevs = mSEDevs;
	}

	public List<double[]> getMSETs() {
		return MSETs;
	}

	public void setMSETs(List<double[]> mSETs) {
		MSETs = mSETs;
	}

	
//	//Evaluation Function
//	public static void evalR(MultiDataSetIterator iter, ComputationGraph net, int index) {
//		
//		 RegressionEvaluation e = new RegressionEvaluation(1);
//		
//		 iter.reset();
//		 
//		 while(iter.hasNext()) {
//			 MultiDataSet data = iter.next();
//			 INDArray input = data.getFeatures(0);
////			 System.out.println("Input 1:" + data.getFeatureMatrix().getRow(0));
////			 System.out.println("Input 2:" + data.getFeatureMatrix().getRow(1));
//	//		 System.out.println("Input Shapre" + input.shapeInfoToString());
//			 INDArray[] output = net.output(input);
//	//		 System.out.println("Label: " + data.getLabels());
//	//		 System.out.println("Prediction: " + output[0]);
//			 e.eval(data.getLabels(index), output[index]);
//		 }
//		 
//		 iter.reset();
//		 
//		 System.out.println("R is: " + String.format("%.4f", e.correlationR2(0))); 
////		 System.out.println("cost is: " + String.format("%.4f", e.meanAbsoluteError(0))); 
//
//		 
//	}
//	
//    public static void eval(INDArray labels, INDArray predictions) {
//        //References for the calculations is this section:
//        //https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#Online_algorithm
//        //https://en.wikipedia.org/wiki/Pearson_product-moment_correlation_coefficient#For_a_sample
//        //Doing online calculation of means, sum of squares, etc.
//
//        labelsSumPerColumn.addi(labels.sum(0));
//
//        INDArray error = predictions.sub(labels);
//        INDArray absErrorSum = Nd4j.getExecutioner().execAndReturn(new Abs(error.dup())).sum(0);
//        INDArray squaredErrorSum = error.mul(error).sum(0);
//
//        sumAbsErrorsPerColumn.addi(absErrorSum);
//        sumSquaredErrorsPerColumn.addi(squaredErrorSum);
//
//        sumOfProducts.addi(labels.mul(predictions).sum(0));
//
//        sumSquaredLabels.addi(labels.mul(labels).sum(0));
//        sumSquaredPredicted.addi(predictions.mul(predictions).sum(0));
//
//        int nRows = labels.size(0);
//
//        currentMean.muli(exampleCount).addi(labels.sum(0)).divi(exampleCount + nRows);
//        currentPredictionMean.muli(exampleCount).addi(predictions.sum(0)).divi(exampleCount + nRows);
//
//        exampleCount += nRows;
//    }
//    
//    public static double correlationR2(int column) {
//        //r^2 Correlation coefficient
//
//        double sumxiyi = sumOfProducts.getDouble(column);
//        double predictionMean = currentPredictionMean.getDouble(column);
//        double labelMean = currentMean.getDouble(column);
//
//        double sumSquaredLabels = sumSquaredLabels.getDouble(column);
//        double sumSquaredPredicted = sumSquaredPredicted.getDouble(column);
//
//        double r2 = sumxiyi - exampleCount * predictionMean * labelMean;
//        r2 /= Math.sqrt(sumSquaredLabels - exampleCount * labelMean * labelMean)
//                        * Math.sqrt(sumSquaredPredicted - exampleCount * predictionMean * predictionMean);
//
//        return r2;
//    }
//	
}