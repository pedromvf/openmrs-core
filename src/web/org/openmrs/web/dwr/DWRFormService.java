package org.openmrs.web.dwr;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.ConceptName;
import org.openmrs.ConceptWord;
import org.openmrs.Field;
import org.openmrs.Form;
import org.openmrs.FormField;
import org.openmrs.api.ConceptService;
import org.openmrs.api.FormService;
import org.openmrs.api.context.Context;
import org.openmrs.formentry.FormUtil;
import org.openmrs.web.WebConstants;
import org.openmrs.web.WebUtil;

import uk.ltd.getahead.dwr.WebContextFactory;

public class DWRFormService {

	protected final Log log = LogFactory.getLog(getClass());
	
	public Field getField(Integer fieldId) {
		Context context = (Context) WebContextFactory.get().getSession().getAttribute(WebConstants.OPENMRS_CONTEXT_HTTPSESSION_ATTR);
		Field f = new Field();
		if (context != null) {
			FormService fs = context.getFormService();
			f = fs.getField(fieldId);
		}
		return f;
	}
	
	public FormFieldListItem getFormField(Integer formFieldId) {
		Context context = (Context) WebContextFactory.get().getSession().getAttribute(WebConstants.OPENMRS_CONTEXT_HTTPSESSION_ATTR);
		FormField f = new FormField();
		if (context != null) {
			FormService fs = context.getFormService();
			f = fs.getFormField(formFieldId);
		}
		return new FormFieldListItem(f, context.getLocale());
	}

	public List<FieldListItem> findFields(String txt) {
		Context context = (Context) WebContextFactory.get().getSession().getAttribute(WebConstants.OPENMRS_CONTEXT_HTTPSESSION_ATTR);
		
		List<FieldListItem> fields = new Vector<FieldListItem>();
		
		if (context != null) {
			for(Field field : context.getFormService().findFields(txt))
				fields.add(new FieldListItem(field, context.getLocale()));
		}
		
		return fields;
	}
	
	public List<Object> findFieldsAndConcepts(String txt) {
		Context context = (Context) WebContextFactory.get().getSession().getAttribute(WebConstants.OPENMRS_CONTEXT_HTTPSESSION_ATTR);
		
		Locale locale = context.getLocale();
		
		// return list will contain ConceptListItems and FieldListItems.
		List<Object> objects = new Vector<Object>();
		
		if (context != null) {

			Concept concept = null;
			try {
				Integer i = Integer.valueOf(txt);
				concept = context.getConceptService().getConcept(i);
			}
			catch (NumberFormatException e) {}
			
			Map<Integer, Boolean> fieldForConceptAdded = new HashMap<Integer, Boolean>();
			
			if (concept != null) {
				for (Field field : context.getFormService().findFields(concept)) {
					FieldListItem fli = new FieldListItem(field, locale); 
					if (!objects.contains(fli))
						objects.add(fli);
					fieldForConceptAdded.put(concept.getConceptId(), true);
				}
				if (!fieldForConceptAdded.containsKey((concept.getConceptId()))) {
					objects.add(new ConceptListItem(concept, locale));
					fieldForConceptAdded.put(concept.getConceptId(), true);
				}
				
			}
			
			for(Field field : context.getFormService().findFields(txt)) {
				FieldListItem fi = new FieldListItem(field, locale);
				if (!objects.contains(fi)) {
					objects.add(fi);
					concept = field.getConcept();
					if (concept != null)
						fieldForConceptAdded.put(concept.getConceptId(), true);
				}
				
			}
			
			List<ConceptWord> conceptWords = context.getConceptService().findConcepts(txt, locale, false);
			for (ConceptWord word : conceptWords) {
				concept = word.getConcept();
				for (Field field : context.getFormService().findFields(concept)) {
					FieldListItem fli = new FieldListItem(field, locale);
					if (!objects.contains(fli))
						objects.add(fli);
					fieldForConceptAdded.put(concept.getConceptId(), true);
				}
				if (!fieldForConceptAdded.containsKey((concept.getConceptId()))) {
					objects.add(new ConceptListItem(word));
					fieldForConceptAdded.put(concept.getConceptId(), true);
				}
			}

			Collections.sort(objects, new FieldConceptSort<Object>(locale));
			
		}
		
		return objects;
	}
	
	public String getJSTree(Integer formId) {
		Context context = (Context) WebContextFactory.get().getSession().getAttribute(WebConstants.OPENMRS_CONTEXT_HTTPSESSION_ATTR);
		if (context != null) {
			Form form = context.getFormService().getForm(formId);
			TreeMap<Integer, TreeSet<FormField>> formFields = FormUtil.getFormStructure(context, form);
			return generateJSTree(formFields, 0, context.getLocale());
		}
		return "";
	}
	
	public Integer[] saveFormField(Integer fieldId, String name, String fieldDesc, Integer fieldTypeId, Integer conceptId, String table, String attr, 
			String defaultValue, boolean multiple, Integer formFieldId, Integer formId, Integer parent, Integer number, String part, Integer page, Integer min, Integer max, boolean required, float sortWeight) {
		
		Context context = (Context) WebContextFactory.get().getSession().getAttribute(WebConstants.OPENMRS_CONTEXT_HTTPSESSION_ATTR);
		
		FormField ff = null;
		Field field = null;
		
		if (context != null && context.isAuthenticated()) {
			FormService fs = context.getFormService();
			ConceptService cs = context.getConceptService();
			
			
			if (formFieldId != null && formFieldId != 0)
				ff = fs.getFormField(formFieldId);
			else
				ff = new FormField(formFieldId);
			
			ff.setForm(fs.getForm(formId));
			if (parent == null)
				ff.setParent(null);
			else if (!parent.equals(ff.getFormFieldId()))
				ff.setParent(fs.getFormField(parent));
			ff.setFieldNumber(number);
			ff.setFieldPart(part);
			ff.setPageNumber(page);
			ff.setMinOccurs(min);
			ff.setMaxOccurs(max);
			ff.setRequired(required);
			ff.setSortWeight(sortWeight);
			
			log.debug("fieldId: " + fieldId);
			log.debug("formFieldId: " + formFieldId);
			log.debug("parentId: "+ parent);
			log.debug("parent: " + ff.getParent());
			
			if (fieldId != null && fieldId != 0)
				field = fs.getField(fieldId);
			else
				field = new Field(fieldId);
			
			if (field == null) {
				log.error("Field is null. Field Id: " + fieldId);
			}
			
			field.setName(name);
			field.setDescription(fieldDesc);
			field.setFieldType(fs.getFieldType(fieldTypeId));
			if (conceptId != null && conceptId != 0)
				field.setConcept(cs.getConcept(conceptId));
			else
				field.setConcept(null);
			field.setTableName(table);
			field.setAttributeName(attr);
			field.setDefaultValue(defaultValue);
			field.setSelectMultiple(multiple);
		
			ff.setField(field);
			
			fs.updateFormField(ff);
			formFieldId = ff.getFormFieldId();
			
			context.endTransaction();
		}
		else {
			log.error("User is unauthenticated for this");
			Integer[] arr = {0};
			return arr;
		}
		
		Integer[] arr = {field.getFieldId(), ff.getFormFieldId()};
		
		return arr;
	}
	
	public void deleteFormField(Integer id) {
		Context context = (Context) WebContextFactory.get().getSession().getAttribute(WebConstants.OPENMRS_CONTEXT_HTTPSESSION_ATTR);
		if (context != null && context.isAuthenticated()) {
			context.getFormService().deleteFormField(context.getFormService().getFormField(id));
			context.endTransaction();
		}
	}
    
    private String generateJSTree(TreeMap<Integer, TreeSet<FormField>> formFields, Integer current, Locale locale) {
		
		String s = "";
		
		if (formFields.containsKey(current)) {
			TreeSet<FormField> set = formFields.get(current);
			for (FormField ff : set) {
				s += generateFormFieldJavascript(ff, locale);
				if (formFields.containsKey(ff.getFormFieldId())) {
					s += generateJSTree(formFields, ff.getFormFieldId(), locale);
				}
			}
		}
		
		return s;
	}
    
    private String generateFormFieldJavascript(FormField ff, Locale locale) {
    	
    	String parent = "''";
		if (ff.getParent() != null)
			parent = ff.getParent().getFormFieldId().toString();
		
		Field field = ff.getField();
		Concept concept = new Concept();
		ConceptName conceptName = new ConceptName();
		Boolean isSet = false;
		if (field.getConcept() != null) {
			concept = field.getConcept();
			conceptName = concept.getName(locale);
			isSet = concept.isSet();
		}
		
    	return "addNode(tree, {formFieldId: " + ff.getFormFieldId() + ", " + 
    					"parent: " + parent + ", " + 
    					"fieldId: " + field.getFieldId() + ", " + 
    					"fieldName: \"" + WebUtil.escapeQuotes(field.getName()) + "\", " + 
    					"description: \"" + WebUtil.escapeQuotes(field.getDescription()) + "\", " +
    					"fieldType: " + field.getFieldType().getFieldTypeId() + ", " + 
    					"conceptId: " + concept.getConceptId() + ", " + 
						"conceptName: \"" + WebUtil.escapeQuotes(conceptName.getName()) + "\", " + 
    					"tableName: \"" + field.getTableName() + "\", " + 
    					"attributeName: \"" + field.getAttributeName() + "\", " + 
    					"defaultValue: \"" + WebUtil.escapeQuotes(field.getDefaultValue()) + "\", " + 
    					"selectMultiple: " + field.getSelectMultiple() + ", " + 
    					"numForms: " + field.getForms().size() + ", " + 
    					"isSet: " + isSet + ", " +
    						
    					"fieldNumber: " + ff.getFieldNumber() + ", " + 
    					"fieldPart: \"" + (ff.getFieldPart() == null ? "" : WebUtil.escapeQuotes(ff.getFieldPart())) + "\", " + 
    					"pageNumber: " + ff.getPageNumber() + ", " + 
    					"minOccurs: " + ff.getMinOccurs() + ", " + 
    					"maxOccurs: " + ff.getMaxOccurs() + ", " + 
    					"isRequired: " + ff.isRequired() + ", " + 
    					"sortWeight: " + ff.getSortWeight() + "});";
    }
    
    /**
     * Sorts loosely on:
     *   FieldListItems first, then concepts
     *   FieldListItems with higher number of forms first, then lower
     *   Concepts with shorter names before longer names
     * @author bwolfe
     *
     * @param <Obj>
     */
    
    private class FieldConceptSort<Obj extends Object> implements Comparator<Object> {
		Locale locale;
		FieldConceptSort(Locale locale) {
			this.locale = locale;
		}
		public int compare(Object o1, Object o2) {
			if (o1 instanceof FieldListItem && o2 instanceof FieldListItem) {
				FieldListItem f1 = (FieldListItem)o1;
				FieldListItem f2 = (FieldListItem)o2;
				Integer numForms1 = f1.getNumForms();
				Integer numForms2 = f2.getNumForms();
				return numForms2.compareTo(numForms1);
			}
			else if (o1 instanceof FieldListItem && o2 instanceof ConceptListItem) {
				return -1;
			}
			else if (o1 instanceof ConceptListItem && o2 instanceof FieldListItem) {
				return 1;
			}
			else if (o1 instanceof ConceptListItem && o2 instanceof ConceptListItem) {
				ConceptListItem c1 = (ConceptListItem)o1;
				ConceptListItem c2 = (ConceptListItem)o2;
				int length1 = c1.getName().length();
				int length2 = c2.getName().length();
				return new Integer(length1).compareTo(new Integer(length2));
			}
			else
				return 0;
		}
    }
}
