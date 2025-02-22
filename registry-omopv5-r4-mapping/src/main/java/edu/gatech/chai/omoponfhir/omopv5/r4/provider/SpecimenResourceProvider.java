/*******************************************************************************
 * Copyright (c) 2019 Georgia Tech Research Institute
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
 *******************************************************************************/
package edu.gatech.chai.omoponfhir.omopv5.r4.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Specimen;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import edu.gatech.chai.omoponfhir.omopv5.r4.mapping.OmopSpecimen;
import edu.gatech.chai.omoponfhir.omopv5.r4.utilities.ThrowFHIRExceptions;
import edu.gatech.chai.omopv5.dba.service.ParameterWrapper;

public class SpecimenResourceProvider implements IResourceProvider {

	private WebApplicationContext myAppCtx;
	private OmopSpecimen myMapper;
	private int preferredPageSize = 30;

	public SpecimenResourceProvider() {
		myAppCtx = ContextLoaderListener.getCurrentWebApplicationContext();
		myMapper = new OmopSpecimen(myAppCtx);
	}
	
	public static String getType() {
		return "Specimen";
	}

	public OmopSpecimen getMyMapper() {
		return myMapper;
	}
	
	private Integer getTotalSize(List<ParameterWrapper> paramList) throws Exception {
		final Long totalSize;
		if (paramList.isEmpty()) {
			totalSize = getMyMapper().getSize();
		} else {
			totalSize = getMyMapper().getSize(paramList);
		}
		
		return totalSize.intValue();
	}


	/**
	 * The "@Create" annotation indicates that this method implements "create=type", which adds a 
	 * new instance of a resource to the server.
	 * @throws Exception 
	 */
	@Create()
	public MethodOutcome createSpecimen(@ResourceParam Specimen theSpecimen) throws Exception {
		validateResource(theSpecimen);
		
		Long id = null;
		try {
			id = getMyMapper().toDbase(theSpecimen, null);
		} catch (FHIRException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (id == null) {
			OperationOutcome outcome = new OperationOutcome();
			CodeableConcept detailCode = new CodeableConcept();
			detailCode.setText("Failed to create entity.");
			outcome.addIssue().setSeverity(IssueSeverity.FATAL).setDetails(detailCode);
			throw new UnprocessableEntityException(FhirContext.forDstu3(), outcome);
		}

		return new MethodOutcome(new IdDt(id));
	}

	@Delete()
	public void deleteObservation(@IdParam IdType theId) throws Exception {
		if (getMyMapper().removeByFhirId(theId) <= 0) {
			throw new ResourceNotFoundException(theId);
		}
	}

	@Search()
	public IBundleProvider findSpecimenssById(
			@RequiredParam(name=Specimen.SP_RES_ID) TokenParam theSpecimenId,
			@Sort SortSpec theSort,

			@IncludeParam(allow={"Specimen:subject"})
			final Set<Include> theIncludes,
			
			@IncludeParam(reverse=true)
            final Set<Include> theReverseIncludes
			) throws Exception {
		List<ParameterWrapper> paramList = new ArrayList<ParameterWrapper> ();

		if (theSpecimenId != null) {
			paramList.addAll(getMyMapper().mapParameter (Specimen.SP_RES_ID, theSpecimenId, false));
		}

		String orderParams = getMyMapper().constructOrderParams(theSort);

		MyBundleProvider myBundleProvider = new MyBundleProvider(paramList, theIncludes, theReverseIncludes);
		myBundleProvider.setTotalSize(getTotalSize(paramList));
		myBundleProvider.setPreferredPageSize(preferredPageSize);
		myBundleProvider.setOrderParams(orderParams);
		return myBundleProvider;
	}
	
	@Search()
	public IBundleProvider findSpecimensByParams(
			@OptionalParam(name=Specimen.SP_COLLECTED) DateRangeParam theRangeDate,
			@OptionalParam(name=Specimen.SP_PATIENT, chainWhitelist={"", Patient.SP_NAME, Patient.SP_IDENTIFIER}) ReferenceParam thePatient,
			@OptionalParam(name=Specimen.SP_SUBJECT, chainWhitelist={"", Patient.SP_NAME, Patient.SP_IDENTIFIER}) ReferenceParam theSubject,
			@Sort SortSpec theSort,

			@IncludeParam(allow={"Specimen:subject"})
			final Set<Include> theIncludes,
			
			@IncludeParam(reverse=true)
            final Set<Include> theReverseIncludes
			) throws Exception {		
		List<ParameterWrapper> paramList = new ArrayList<ParameterWrapper> ();
		
		if (theRangeDate != null) {
			paramList.addAll(getMyMapper().mapParameter(Specimen.SP_COLLECTED, theRangeDate, false));
		}
		
		// With OMOP, we only support subject to be patient.
		// If the subject has only ID part, we assume that is patient.
		if (theSubject != null) {
			if (theSubject.getResourceType() != null && 
					theSubject.getResourceType().equals(PatientResourceProvider.getType())) {
				thePatient = theSubject;
			} else {
				// If resource is null, we assume Patient.
				if (theSubject.getResourceType() == null) {
					thePatient = theSubject;
				} else {
					ThrowFHIRExceptions.unprocessableEntityException("subject search allows Only Patient Resource, but provided "+theSubject.getResourceType());
				}
			}
		}
		
		if (thePatient != null) {
			String patientChain = thePatient.getChain();
			if (patientChain != null) {
				if (Patient.SP_NAME.equals(patientChain)) {
					String thePatientName = thePatient.getValue();
					paramList.addAll(getMyMapper().mapParameter ("Patient:"+Patient.SP_NAME, thePatientName, false));
				} else if (Patient.SP_IDENTIFIER.equals(patientChain)) {
					paramList.addAll(getMyMapper().mapParameter ("Patient:"+Patient.SP_IDENTIFIER, thePatient.getValue(), false));
				} else if ("".equals(patientChain)) {
					paramList.addAll(getMyMapper().mapParameter ("Patient:"+Patient.SP_RES_ID, thePatient.getValue(), false));
				}
			} else {
				paramList.addAll(getMyMapper().mapParameter ("Patient:"+Patient.SP_RES_ID, thePatient.getIdPart(), false));
			}
		}
		
		String orderParams = getMyMapper().constructOrderParams(theSort);

		MyBundleProvider myBundleProvider = new MyBundleProvider(paramList, theIncludes, theReverseIncludes);
		myBundleProvider.setTotalSize(getTotalSize(paramList));
		myBundleProvider.setPreferredPageSize(preferredPageSize);
		myBundleProvider.setOrderParams(orderParams);
		return myBundleProvider;
	}
	
	/**
	 * This is the "read" operation. The "@Read" annotation indicates that this method supports the read and/or vread operation.
	 * <p>
	 * Read operations take a single parameter annotated with the {@link IdParam} paramater, and should return a single resource instance.
	 * </p>
	 * 
	 * @param theId
	 *            The read operation takes one parameter, which must be of type IdDt and must be annotated with the "@Read.IdParam" annotation.
	 * @return Returns a resource matching this identifier, or null if none exists.
	 * @throws Exception 
	 */
	@Read()
	public Specimen readSpecimen(@IdParam IdType theId) throws Exception {
		Specimen retval = (Specimen) getMyMapper().toFHIR(theId);
		if (retval == null) {
			throw new ResourceNotFoundException(theId);
		}
			
		return retval;
	}

	/**
	 * The "@Update" annotation indicates that this method supports replacing an existing 
	 * resource (by ID) with a new instance of that resource.
	 * 
	 * @param theId
	 *            This is the ID of the patient to update
	 * @param thePatient
	 *            This is the actual resource to save
	 * @return This method returns a "MethodOutcome"
	 * @throws Exception 
	 */
	@Update()
	public MethodOutcome updateSpecimen(@IdParam IdType theId, @ResourceParam Specimen theSpecimen) throws Exception {
		validateResource(theSpecimen);
		
		Long fhirId=null;
		try {
			fhirId = getMyMapper().toDbase(theSpecimen, theId);
		} catch (FHIRException e) {
			e.printStackTrace();
		}

		if (fhirId == null) {
			throw new ResourceNotFoundException(theId);
		}

		return new MethodOutcome();
	}
	
	// TODO: Add validation code here.
	private void validateResource(Specimen theSpecimen) {
		if (theSpecimen.getSubject() == null || theSpecimen.getSubject().isEmpty()) {
			throw new FHIRException("Specimen.subject is required in FHIR to OMOP mapping.");
		}

		if (!theSpecimen.getSubject().getReferenceElement().getResourceType().equalsIgnoreCase(PatientResourceProvider.getType())) {
			throw new FHIRException("Specimen only supports " + PatientResourceProvider.getType()
					+ " for subject. But provided [" + theSpecimen.getSubject().getReferenceElement().getResourceType() + "]");
		}

		if (theSpecimen.getCollection().isEmpty()) {
			throw new FHIRException("Specimen.collection is required in FHIR to OMOP mapping.");
		}

		if (theSpecimen.getCollection().getCollected() == null || !(theSpecimen.getCollection().getCollected() instanceof DateTimeType)) {
			throw new FHIRException("Specimen.collection.collectedDateTiem is required in FHIR to OMOP mapping.");
		}
	}

	@Override
	public Class<Specimen> getResourceType() {
		return Specimen.class;
	}

	class MyBundleProvider extends OmopFhirBundleProvider {
		Set<Include> theIncludes;
		Set<Include> theReverseIncludes;

		public MyBundleProvider(List<ParameterWrapper> paramList, Set<Include> theIncludes, Set<Include>theReverseIncludes) {
			super(paramList);
			setPreferredPageSize (preferredPageSize);
			this.theIncludes = theIncludes;
			this.theReverseIncludes = theReverseIncludes;
		}

		@Override
		public List<IBaseResource> getResources(int fromIndex, int toIndex) {
			List<IBaseResource> retv = new ArrayList<IBaseResource>();
			
			// _Include
			List<String> includes = new ArrayList<String>();

			if (theIncludes.contains(new Include("Specimen:subject"))) {
				includes.add("Specimen:subject");
			}

			try {
				if (paramList.size() == 0) {
					getMyMapper().searchWithoutParams(fromIndex, toIndex, retv, includes, orderParams);
				} else {
					getMyMapper().searchWithParams(fromIndex, toIndex, paramList, retv, includes, orderParams);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return retv;
		}
	}
}
