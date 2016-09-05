package org.matsim.utils.objectattributes.attributable;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.matsim.utils.objectattributes.attributeconverters.*;

import java.util.*;

/**
 * This class is meant to be used as a delegate by any reader that reads an {@link Attributable} object
 * @author thibautd
 */
public class AttributesXmlReaderDelegate {
	private final static Logger log = Logger.getLogger(AttributesXmlReaderDelegate.class);
	private final Map<String, AttributeConverter<?>> converters = new HashMap<String, AttributeConverter<?>>();
	private boolean readCharacters = false;

	private Attributes currentAttributes = null;
	private String currentAttribute = null;
	private String currentAttributeClass = null;

	/*package*/ final static String TAG_ATTRIBUTES = "attributes";
	/*package*/ final static String TAG_ATTRIBUTE = "attribute";
	/*package*/ final static String ATTR_ATTRIBUTENAME = "name";
	/*package*/ final static String ATTR_ATTRIBUTECLASS = "class";

	private static final StringConverter STRING_Converter = new StringConverter();
	private static final IntegerConverter INTEGER_Converter = new IntegerConverter();
	private static final FloatConverter FLOAT_Converter = new FloatConverter();
	private static final DoubleConverter DOUBLE_Converter = new DoubleConverter();
	private static final BooleanConverter BOOLEAN_Converter = new BooleanConverter();
	private static final LongConverter LONG_Converter = new LongConverter();

	private final Set<String> missingConverters = new HashSet<String>();

	public AttributesXmlReaderDelegate() {
		this.converters.put(String.class.getCanonicalName(), STRING_Converter);
		this.converters.put(Integer.class.getCanonicalName(), INTEGER_Converter);
		this.converters.put(Float.class.getCanonicalName(), FLOAT_Converter);
		this.converters.put(Double.class.getCanonicalName(), DOUBLE_Converter);
		this.converters.put(Boolean.class.getCanonicalName(), BOOLEAN_Converter);
		this.converters.put(Long.class.getCanonicalName(), LONG_Converter);
	}

	public void startTag(String name,
						 org.xml.sax.Attributes atts,
						 Stack<String> context,
						 Attributes currentAttributes ) {
		if (TAG_ATTRIBUTE.equals(name)) {
			this.currentAttribute = atts.getValue(ATTR_ATTRIBUTENAME);
			this.currentAttributeClass = atts.getValue(ATTR_ATTRIBUTECLASS);
			this.readCharacters = true;
		} else if (TAG_ATTRIBUTES.equals(name)) {
			this.currentAttributes = currentAttributes;
		}
	}

	public void endTag(String name, String content, Stack<String> context) {
		if (TAG_ATTRIBUTE.equals(name)) {
			this.readCharacters = false;
			AttributeConverter<?> c = this.converters.get(this.currentAttributeClass);
			if (c == null) {
				if (missingConverters.add(this.currentAttributeClass)) {
					log.warn("No AttributeConverter found for class " + this.currentAttributeClass + ". Not all attribute values can be read.");
				}
			} else {
				Object o = this.converters.get(this.currentAttributeClass).convert(content);
				this.currentAttributes.putAttribute( this.currentAttribute, o);
			}
		}
	}

	public Attributes getCurrentAttributes() {
		return currentAttributes;
	}

	/**
	 * Sets the converter for reading attributes of the specified class.
	 *
	 * @param clazz
	 * @param converter
	 * @return the previously registered converter for this class, or <code>null</code> if none was set before.
	 */
	public AttributeConverter<?> putAttributeConverter(final Class<?> clazz, final AttributeConverter<?> converter) {
		return this.converters.put(clazz.getCanonicalName(), converter);
	}

	@Inject
	public void putAttributeConverters( final Map<Class<?>, AttributeConverter<?>> converters ) {
		for ( Map.Entry<Class<?>, AttributeConverter<?>> e : converters.entrySet() ) {
			putAttributeConverter( e.getKey() , e.getValue() );
		}
	}

	/**
	 * Removes the converter for reading attributes of the specified class.
	 *
	 * @param clazz
	 * @return the previously registered converter for this class, of <code>null</code> if none was set.
	 */
	public AttributeConverter<?> removeAttributeConverter(final Class<?> clazz) {
		return this.converters.remove(clazz.getCanonicalName());
	}
}