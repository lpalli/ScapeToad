package ch.epfl.scapetoad.gui;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollectionWrapper;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;

import ch.epfl.scapetoad.CartogramFeature;
import ch.epfl.scapetoad.CartogramLayer;

/**
 * Utilities class to convert between JUMP and cartogram objects.
 * 
 * @author luca@palli.ch
 */
public class Utils {

    /**
     * Convert a list of JUMP features to a list of cartogram features.
     * 
     * @param aFeatures
     *            the list of features to convert
     * @return the list of converted features
     */
    public static List<CartogramFeature> convert(List<Feature> aFeatures) {
        List<CartogramFeature> features = new ArrayList<CartogramFeature>(
                aFeatures.size());
        for (Feature feature : aFeatures) {
            features.add(Utils.convert(feature));
        }
        return features;
    }

    /**
     * Convert a list of cartogram features to a list of JUMP features.
     * 
     * @param aFeatures
     *            the list of cartogram features
     * @param aSchema
     *            the JUMP schema
     * @return the list of converted features
     */
    private static List<Feature> convert(List<CartogramFeature> aFeatures,
            FeatureSchema aSchema) {
        List<Feature> features = new ArrayList<Feature>(aFeatures.size());
        for (CartogramFeature feature : aFeatures) {
            features.add(Utils.convert(feature, aSchema));
        }
        return features;
    }

    /**
     * Convert a cartogram layer to a JUMP cartogram
     * 
     * @param aLayer
     *            the layer to convert
     * @param aLayerManager
     *            the layer manager
     * @return the converted layer
     */
    public static Layer convert(CartogramLayer aLayer,
            LayerManager aLayerManager) {
        FeatureSchema schema = new FeatureSchema();
        for (@SuppressWarnings("rawtypes")
        Map.Entry<String, Class> attribute : aLayer.getAttributes().entrySet()) {
            if (attribute.getValue() == Date.class) {
                schema.addAttribute(attribute.getKey(), AttributeType.DATE);
            } else if (attribute.getValue() == Double.class) {
                schema.addAttribute(attribute.getKey(), AttributeType.DOUBLE);
            } else if (attribute.getValue() == Geometry.class) {
                schema.addAttribute(attribute.getKey(), AttributeType.GEOMETRY);
            } else if (attribute.getValue() == Integer.class) {
                schema.addAttribute(attribute.getKey(), AttributeType.INTEGER);
            } else if (attribute.getValue() == Object.class) {
                schema.addAttribute(attribute.getKey(), AttributeType.OBJECT);
            } else if (attribute.getValue() == String.class) {
                schema.addAttribute(attribute.getKey(), AttributeType.STRING);
            }
        }

        return new Layer(aLayer.getName(), aLayer.getColor(),
                new FeatureDataset(Utils.convert(aLayer.getFeatures(), schema),
                        schema), aLayerManager);
    }

    /**
     * Convert a JUMP layer to a cartogram cartogram
     * 
     * @param aLayer
     *            the layer to convert
     * @return the converted layer
     */
    @SuppressWarnings("unchecked")
    public static CartogramLayer convert(Layer aLayer) {
        FeatureCollectionWrapper wrapper = aLayer.getFeatureCollectionWrapper();
        return new CartogramLayer(aLayer.getName(), aLayer.getBasicStyle()
                .getFillColor(), Utils.convert(wrapper.getFeatureSchema()),
                Utils.convert(wrapper.getFeatures()));
    }

    /**
     * Convert a JUMP feature to a cartogram feature.
     * 
     * @param aFeature
     *            the feature to convert
     * @return the converted feature
     */
    private static CartogramFeature convert(Feature aFeature) {
        FeatureSchema schema = aFeature.getSchema();
        int size = schema.getAttributeCount();
        Hashtable<String, Object> attributes = new Hashtable<String, Object>(
                size);

        Object value;
        for (int i = 0; i < size; i++) {
            value = aFeature.getAttribute(i);
            if (value != null) {
                attributes.put(schema.getAttributeName(i), value);
            }
        }

        return new CartogramFeature(aFeature.getGeometry(), attributes);
    }

    /**
     * Convert a cartogram feature to a JUMP feature.
     * 
     * @param aFeature
     *            the feature to convert
     * @param aSchema
     *            the JUMP schema
     * @return the converted feature
     */
    private static Feature convert(CartogramFeature aFeature,
            FeatureSchema aSchema) {
        Feature feature = new BasicFeature(aSchema);
        feature.setSchema(aSchema);
        feature.setGeometry(feature.getGeometry());

        int size = aSchema.getAttributeCount();
        for (int i = 0; i < size; i++) {
            feature.setAttribute(i,
                    aFeature.getAttribute(aSchema.getAttributeName(i)));
        }

        return feature;
    }

    /**
     * Convert a JUMP schema to an attribute list.
     * 
     * @param aSchema
     *            the schema
     * @return the attribute list
     */
    @SuppressWarnings("rawtypes")
    public static Hashtable<String, Class> convert(FeatureSchema aSchema) {
        int size = aSchema.getAttributeCount();
        Hashtable<String, Class> attributes = new Hashtable<String, Class>(size);

        AttributeType type;
        for (int i = 0; i < size; i++) {
            type = aSchema.getAttributeType(i);
            if (type == AttributeType.DATE) {
                attributes.put(aSchema.getAttributeName(i), Date.class);
            } else if (type == AttributeType.DOUBLE) {
                attributes.put(aSchema.getAttributeName(i), Double.class);
            } else if (type == AttributeType.GEOMETRY) {
                attributes.put(aSchema.getAttributeName(i), Geometry.class);
            } else if (type == AttributeType.INTEGER) {
                attributes.put(aSchema.getAttributeName(i), Integer.class);
            } else if (type == AttributeType.OBJECT) {
                attributes.put(aSchema.getAttributeName(i), Object.class);
            } else if (type == AttributeType.STRING) {
                attributes.put(aSchema.getAttributeName(i), String.class);
            }
        }

        return attributes;
    }
}