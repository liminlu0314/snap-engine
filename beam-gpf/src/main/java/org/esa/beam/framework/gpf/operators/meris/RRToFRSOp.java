/*
 * $Id: L3ToL1Op.java,v 1.1 2007/03/27 12:51:05 marcoz Exp $
 *
 * Copyright (C) 2007 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.gpf.operators.meris;

import java.awt.Rectangle;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.AbstractOperator;
import org.esa.beam.framework.gpf.AbstractOperatorSpi;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Raster;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;

import com.bc.ceres.core.ProgressMonitor;

/**
 * Created by marcoz.
 *
 * @author marcoz
 * @version $Revision: 1.1 $ $Date: 2007/03/27 12:51:05 $
 */
public class RRToFRSOp extends AbstractOperator {

    private GeoCoding rrGeoCoding;
    private GeoCoding frsGeoCoding;
    
    @SourceProduct(alias="frs")
    private Product frsProduct;
    @SourceProduct(alias="rr")
    private Product rrProduct;
    @TargetProduct
    private Product targetProduct;

    public RRToFRSOp(OperatorSpi spi) {
        super(spi);
    }

    @Override
	protected Product initialize(ProgressMonitor pm) throws OperatorException {
        rrGeoCoding = rrProduct.getGeoCoding();
        frsGeoCoding = frsProduct.getGeoCoding();

        final int width = frsProduct.getSceneRasterWidth();
        final int height = frsProduct.getSceneRasterHeight();
        targetProduct = new Product("L1", "L1", width, height);

        Band[] srcBands = rrProduct.getBands();
        for (Band sourceBand : srcBands) {
            Band targetBand = targetProduct.addBand(sourceBand.getName(), sourceBand.getDataType());
            ProductUtils.copySpectralAttributes(sourceBand, targetBand);
            targetBand.setDescription(sourceBand.getDescription());
            targetBand.setUnit(sourceBand.getUnit());
            targetBand.setScalingFactor(sourceBand.getScalingFactor());
            targetBand.setScalingOffset(sourceBand.getScalingOffset());
            targetBand.setLog10Scaled(sourceBand.isLog10Scaled());
            targetBand.setNoDataValueUsed(sourceBand.isNoDataValueUsed());
            targetBand.setNoDataValue(sourceBand.getNoDataValue());
        }
        return targetProduct;
    }

    @Override
    public void computeBand(Raster targetRaster, ProgressMonitor pm) throws OperatorException {
    	
    	Rectangle frsRectangle = targetRaster.getRectangle();
    	Band rrSrcBand = rrProduct.getBand(targetRaster.getRasterDataNode().getName());
    	
    	pm.beginTask("compute", frsRectangle.height);
        
            PixelPos frsPixelPos = new PixelPos();
            PixelPos rrPixelPos = new PixelPos();
            GeoPos geoPos = new GeoPos();
            
            frsPixelPos.y = frsRectangle.y;
            frsPixelPos.x = frsRectangle.x;
            frsGeoCoding.getGeoPos(frsPixelPos, geoPos);
            rrGeoCoding.getPixelPos(geoPos, rrPixelPos);
            Rectangle rrRectangle = new Rectangle(Math.round(rrPixelPos.x), Math.round(rrPixelPos.y), frsRectangle.width/4, frsRectangle.height/4);
            rrRectangle.grow(2, 2);
            Rectangle sceneRectangle = new Rectangle(rrSrcBand.getSceneRasterWidth(), rrSrcBand.getSceneRasterHeight());
            rrRectangle =  rrRectangle.intersection(sceneRectangle);
            
            System.out.println("RR: "+rrRectangle.toString());
            System.out.println("FRS:"+frsRectangle.toString());
            Raster srcRaster = getRaster(rrSrcBand, rrRectangle);
            
            int rrY = rrRectangle.y;
            int rrX = rrRectangle.x;
            int iy = 0;
            int ix = 0;
            
        try {
        	
            for (int y = frsRectangle.y; y < frsRectangle.y + frsRectangle.height; y++) {
            	rrX = rrRectangle.x;
            	ix = 0;
                for (int x = frsRectangle.x; x < frsRectangle.x + frsRectangle.width; x++) {
                	double d = srcRaster.getDouble(rrX, rrY);
                	targetRaster.setDouble(x, y, d);
					if (ix < 3) {
						ix++;
					} else {
						ix = 0;
						rrX++;						
					}
                }
                if (iy < 3) {
                	iy++;					
				} else {
					iy = 0;
					rrY++;
				}
                pm.worked(1);
            }
//        }catch (Exception e) {
//        	e.printStackTrace();
//        	System.out.println("foof");
        } finally {
        	pm.done();
        }
    }

    
    public static class Spi extends AbstractOperatorSpi {
        public Spi() {
            super(RRToFRSOp.class, "RRToFRS");
        }
    }
}