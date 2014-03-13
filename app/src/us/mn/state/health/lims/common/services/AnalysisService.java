/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) ITECH, University of Washington, Seattle WA.  All Rights Reserved.
 */

package us.mn.state.health.lims.common.services;

import org.apache.commons.validator.GenericValidator;
import us.mn.state.health.lims.analysis.valueholder.Analysis;
import us.mn.state.health.lims.dictionary.dao.DictionaryDAO;
import us.mn.state.health.lims.dictionary.daoimpl.DictionaryDAOImpl;
import us.mn.state.health.lims.dictionary.valueholder.Dictionary;
import us.mn.state.health.lims.result.dao.ResultDAO;
import us.mn.state.health.lims.result.daoimpl.ResultDAOImpl;
import us.mn.state.health.lims.result.valueholder.Result;
import us.mn.state.health.lims.test.valueholder.Test;
import us.mn.state.health.lims.typeofsample.util.TypeOfSampleUtil;
import us.mn.state.health.lims.typeofsample.valueholder.TypeOfSample;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class AnalysisService{
    private static final DictionaryDAO dictionaryDAO = new DictionaryDAOImpl();
    private static final ResultDAO resultDAO = new ResultDAOImpl();
    private final Analysis analysis;

    public AnalysisService(Analysis analysis){
        this.analysis = analysis;
    }

    public Analysis getAnalysis(){
        return analysis;
    }
    public String getTestDisplayName( ){
        Test test = getTest();
        String name = test.getDescription();

        TypeOfSample typeOfSample = TypeOfSampleUtil.getTypeOfSampleForTest( test.getId() );

        if( typeOfSample != null && typeOfSample.getId().equals( TypeOfSampleUtil.getTypeOfSampleIdForLocalAbbreviation( "Variable" ))){
            name += "(" + analysis.getSampleTypeName() + ")";
        }

        if( analysis.getParentResult() != null && "M".equals( analysis.getParentResult().getResultType() )){
            Dictionary dictionary = dictionaryDAO.getDictionaryById( analysis.getParentResult().getValue() );
            if( dictionary != null){
                String parentResult = dictionary.getLocalAbbreviation();
                if( GenericValidator.isBlankOrNull( parentResult )){
                    parentResult = dictionary.getDictEntry();
                }
                name = parentResult + " &rarr; " + name;
            }
        }

        return name;
    }

    public String getCSVMultiselectResults(){
        List<Result> existingResults = resultDAO.getResultsByAnalysis( analysis );
        StringBuilder multiSelectBuffer = new StringBuilder();
        for( Result existingResult : existingResults ){
            if( "M".equals( existingResult.getResultType() ) ){
                multiSelectBuffer.append( existingResult.getValue() );
                multiSelectBuffer.append( ',' );
            }
        }

        // remove last ','
        multiSelectBuffer.setLength( multiSelectBuffer.length() - 1 );

        return multiSelectBuffer.toString();
    }

    public Result getQuantifiedResult(){
        List<Result> existingResults = resultDAO.getResultsByAnalysis( analysis );
        List<String> quantifiableResultsIds = new ArrayList<String>(  );
        for( Result existingResult : existingResults ){
            if( "MD".contains( existingResult.getResultType() ) ){
                quantifiableResultsIds.add( existingResult.getId() );
            }
        }

        for( Result existingResult : existingResults ){
            if( !"MD".contains( existingResult.getResultType()) &&
                    existingResult.getParentResult() != null &&
                    quantifiableResultsIds.contains( existingResult.getParentResult().getId()) &&
                    !GenericValidator.isBlankOrNull(existingResult.getValue())){
            return existingResult;
            }
        }

        return null;
    }
    public String getCompletedDateForDisplay(){
        return analysis.getCompletedDateForDisplay();
    }

    public String getAnalysisType(){
        return analysis.getAnalysisType();
    }

    public String getStatusId(){
        return analysis.getStatusId();
    }

    public Boolean getTriggeredReflex(){
        return analysis.getTriggeredReflex();
    }

    public boolean resultIsConclusion(Result currentResult){
        List<Result> results = resultDAO.getResultsByAnalysis(analysis);
        if (results.size() == 1) {
            return false;
        }

        Long testResultId = Long.parseLong(currentResult.getId());
        // This based on the fact that the conclusion is always added
        // after the shared result so if there is a result with a larger id
        // then this is not a conclusion
        for (Result result : results) {
            if (Long.parseLong(result.getId()) > testResultId) {
                return false;
            }
        }

        return true;
    }

    public boolean isParentNonConforming(){
        return QAService.isAnalysisParentNonConforming(analysis);
    }

    public Test getTest(){
        return analysis.getTest();
    }
}