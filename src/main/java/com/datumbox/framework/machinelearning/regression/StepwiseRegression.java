/**
 * Copyright (C) 2013-2015 Vasilis Vryniotis <bbriniotis@datumbox.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datumbox.framework.machinelearning.regression;

import com.datumbox.framework.machinelearning.common.interfaces.StepwiseCompatible;
import com.datumbox.common.dataobjects.Dataset;
import com.datumbox.common.persistentstorage.interfaces.DatabaseConfiguration;
import com.datumbox.common.persistentstorage.interfaces.DatabaseConnector;
import com.datumbox.common.utilities.MapFunctions;
import com.datumbox.framework.machinelearning.common.bases.mlmodels.BaseMLmodel;
import com.datumbox.framework.machinelearning.common.bases.mlmodels.BaseMLregressor;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Implementation of Stepwise Regression algorithm. The method takes as argument
 * an other regression model which uses to estimate the best model. This implementation
 * uses the backwards elimination algorithm.
 *
 * @author Vasilis Vryniotis <bbriniotis@datumbox.com>
 */
public class StepwiseRegression extends BaseMLregressor<StepwiseRegression.ModelParameters, StepwiseRegression.TrainingParameters, BaseMLregressor.ValidationMetrics>  {
    
    private transient BaseMLregressor mlregressor = null;
    
    /**
     * The ModelParameters class stores the coefficients that were learned during
     * the training of the algorithm.
     */
    public static class ModelParameters extends BaseMLregressor.ModelParameters {

        /**
         * Protected constructor which accepts as argument the DatabaseConnector.
         * 
         * @param dbc 
         */
        protected ModelParameters(DatabaseConnector dbc) {
            super(dbc);
        }
        
        //EMPTY Model parameters. It relies on the mlregressor DB instead        
    } 

    /**
     * The TrainingParameters class stores the parameters that can be changed
     * before training the algorithm.
     */
    public static class TrainingParameters extends BaseMLregressor.TrainingParameters {
        
        //primitives/wrappers
        private Integer maxIterations = null;
        
        private Double aout = 0.05;
        
        //Classes
        private Class<? extends BaseMLregressor> regressionClass;

        //Parameter Objects
        private BaseMLregressor.TrainingParameters regressionTrainingParameters;

        //Field Getters/Setters

        /**
         * Getter for the maximum permitted iterations during training.
         * 
         * @return 
         */
        public Integer getMaxIterations() {
            return maxIterations;
        }

        /**
         * Setter for the maximum permitted iterations during training.
         * 
         * @param maxIterations 
         */
        public void setMaxIterations(Integer maxIterations) {
            this.maxIterations = maxIterations;
        }
        
        /**
         * Getter for the threshold of the maximum p-value; a variable must
         * have a p-value less or equal than the threshold to be retained in the 
         * model. Variables with higher p-value than this threshold are removed.
         * 
         * 
         * @return 
         */
        public Double getAout() {
            return aout;
        }
        
        /**
         * Setter for the threshold of the maximum p-value; a variable must
         * have a p-value less or equal than the threshold to be retained in the 
         * model. Variables with higher p-value than this threshold are removed.
         * 
         * @param aout 
         */
        public void setAout(Double aout) {
            this.aout = aout;
        }
        
        /**
         * Getter for the class of the regression algorithm that is used internally
         * by the Stepwise Regression. The regressor must implement the StepwiseCompatible
         * interface in order to be used in the analysis.
         * 
         * @return 
         */
        public Class<? extends BaseMLregressor> getRegressionClass() {
            return regressionClass;
        }
        
        /**
         * Setter for the class of the regression algorithm that is used internally
         * by the Stepwise Regression. The regressor must implement the StepwiseCompatible
         * interface in order to be used in the analysis.
         * 
         * @param regressionClass 
         */
        public void setRegressionClass(Class<? extends BaseMLregressor> regressionClass) {
            if(!StepwiseCompatible.class.isAssignableFrom(regressionClass)) {
                throw new RuntimeException("The regression model is not Stepwise Compatible as it does not calculates the pvalues of the features.");
            }
            this.regressionClass = regressionClass;
        }
        
        /**
         * Getter for the training parameters of the regression algorithm that is
         * used internally by the Stepwise Regression.
         * 
         * @return 
         */
        public BaseMLregressor.TrainingParameters getRegressionTrainingParameters() {
            return regressionTrainingParameters;
        }
        
        /**
         * Setter for the training parameters of the regression algorithm that is
         * used internally by the Stepwise Regression.
         * 
         * @param regressionTrainingParameters 
         */
        public void setRegressionTrainingParameters(BaseMLregressor.TrainingParameters regressionTrainingParameters) {
            this.regressionTrainingParameters = regressionTrainingParameters;
        }
        
    }


    /*
    The no ValidationMetrics Class here!!!!!! The algorithm fetches the
    validations metrics of the mlregressor and cast them to BaseMLregressor.ValidationMetrics.
    */
    //public static class ValidationMetrics extends BaseMLregressor.ValidationMetrics { }
    
    
    /**
     * Public constructor of the algorithm.
     * 
     * @param dbName
     * @param dbConf 
     */
    public StepwiseRegression(String dbName, DatabaseConfiguration dbConf) {
        super(dbName, dbConf, StepwiseRegression.ModelParameters.class, StepwiseRegression.TrainingParameters.class, StepwiseRegression.ValidationMetrics.class, null); //do not define a validator. pass null and overload the kcross validation method to validate with the mlregressor object
    } 
     
    /**
     * Deletes the database of the algorithm. 
     */
    @Override
    public void erase() {
        loadRegressor();
        mlregressor.erase();
        mlregressor = null;
        
        super.erase();
    }
         
    /**
     * Closes all the resources of the algorithm. 
     */
    @Override
    public void close() {
        loadRegressor();
        mlregressor.close();
        mlregressor = null;
        
        super.close();
    }
    
    /**
     * k-Fold Cross Validation is not supported in this algorithm. Run it directly 
     * to the wrapped regressor.
     * 
     * @param trainingData
     * @param trainingParameters
     * @param k
     * @return 
     */
    @Override
    public BaseMLregressor.ValidationMetrics kFoldCrossValidation(Dataset trainingData, TrainingParameters trainingParameters, int k) {
        throw new UnsupportedOperationException("K-fold Cross Validation is not supported. Run it directly to the wrapped regressor."); 
    }
    
    @Override
    protected BaseMLregressor.ValidationMetrics validateModel(Dataset validationData) {
        loadRegressor();
        
        return (BaseMLregressor.ValidationMetrics) mlregressor.validate(validationData);
    }

    @Override
    protected void _fit(Dataset trainingData) {
        TrainingParameters trainingParameters = knowledgeBase.getTrainingParameters();
        
        Integer maxIterations = trainingParameters.getMaxIterations();
        if(maxIterations==null) {
            maxIterations = Integer.MAX_VALUE;
        }
        double aOut = trainingParameters.getAout();
        
        //copy data before starting
        Dataset copiedTrainingData = trainingData.copy();
        
        //backword elimination algorithm
        for(int iteration = 0; iteration<maxIterations ; ++iteration) {
            
            Map<Object, Double> pvalues = runRegression(copiedTrainingData);
            
            if(pvalues.isEmpty()) {
                break; //no more features
            }
            
            //fetch the feature with highest pvalue, excluding constant
            pvalues.remove(Dataset.constantColumnName);
            Map.Entry<Object, Double> maxPvalueEntry = MapFunctions.selectMaxKeyValue(pvalues);
            pvalues=null;
            
            if(maxPvalueEntry.getValue()<=aOut) {
                break; //nothing to remove, the highest pvalue is less than the aOut
            }
            
            
            Set<Object> removedFeatures = new HashSet<>();
            removedFeatures.add(maxPvalueEntry.getKey());
            copiedTrainingData.removeColumns(removedFeatures);
            removedFeatures = null;
            
            if(copiedTrainingData.getVariableNumber()==0) {
                break; //if no more features exit
            }
        }
        
        //once we have the dataset has been cleared from the unnecessary columns train the model once again
        mlregressor = BaseMLmodel.newInstance(trainingParameters.getRegressionClass(), dbName, knowledgeBase.getDbConf()); 
        
        mlregressor.fit(copiedTrainingData, trainingParameters.getRegressionTrainingParameters());
        copiedTrainingData.erase();
        copiedTrainingData = null;
    }

    @Override
    protected void predictDataset(Dataset newData) {
        loadRegressor();
        
        mlregressor.predict(newData);
    }
    
    private void loadRegressor() {
        if(mlregressor==null) {
            //initialize algorithm
            mlregressor = BaseMLmodel.newInstance(knowledgeBase.getTrainingParameters().getRegressionClass(), dbName, knowledgeBase.getDbConf()); 
        }
    }
    
    private Map<Object, Double> runRegression(Dataset trainingData) {
        TrainingParameters trainingParameters = knowledgeBase.getTrainingParameters();
        
        //initialize algorithm
        mlregressor = BaseMLmodel.newInstance(trainingParameters.getRegressionClass(), dbName, knowledgeBase.getDbConf()); 

        //train the regressor
        mlregressor.fit(trainingData, trainingParameters.getRegressionTrainingParameters());

        //get pvalues
        Map<Object, Double> pvalues = ((StepwiseCompatible)mlregressor).getFeaturePvalues();
        mlregressor.erase();
        
        return pvalues;
    }
}
