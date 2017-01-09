/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   27.12.2016 (eike): created
 */
package org.knime.knip.pixml.node.pixfeat2d.ops;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.knime.knip.core.KNIPGateway;

import net.imagej.ImgPlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

/**
 *
 * @author eike
 * @param <T> type
 */
public class FeatureCalculator<T extends RealType<T>> {

    private static String[] availableFeatures =
        new String[]{Feature.GAUSSIAN_BLUR.toString(), "Hessian", "Difference_of_gaussians", "Mean", "Max", "Min"};

    public enum Feature {
            GAUSSIAN_BLUR("Gaussian_blur"), HESSIAN("Hessian"), DIFFERENCE_OF_GAUSSIAN("Difference_of_gaussians"),
            MEAN("Mean"), MAX("Max"), MIN("Min");

        private final String name;

        private Feature(final String feat) {
            this.name = feat;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private Feature[] m_selectedFeatures;

    private Img<T> img;

    private float minSigma;

    private float maxSigma;

    /**
     * constructor
     *
     * @param input image of which features are calculated
     */
    public FeatureCalculator(final ImgPlus<T> input) {
        img = input;
    }

    /**
     * @param index
     * @return feature at index
     */
    public static String getAvailableFeature(final int index) {
        return availableFeatures[index];
    }

    /**
     * @return string array of implemented features
     */
    public static String[] getAvailableFeatures() {
        return availableFeatures;
    }

    public void setSelectedFeatures(final Feature[] features) {
        m_selectedFeatures = features;
    }

    public void setMinSigma(final float sigma) {
        minSigma = sigma;
    }

    public void setMaxSigma(final float sigma) {
        maxSigma = sigma;
    }

    public RandomAccessibleInterval<T> compute() {
        ExecutorService exes = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        if(Thread.currentThread().isInterrupted()) {
            return null;
        }

        List<RandomAccessibleInterval<T>> stack = new ArrayList<>();

        ArrayList<Future<RandomAccessibleInterval<T>>> futures = new ArrayList<>();

        try {

            for(Feature feature : m_selectedFeatures) {
                switch(feature) {
                    case GAUSSIAN_BLUR: break;
                    case DIFFERENCE_OF_GAUSSIAN:
                        break;
                    case HESSIAN:
                        break;
                    case MAX:
                        futures.add(exes.submit(getMax()));
                        break;
                    case MEAN:
                        break;
                    case MIN:
                        break;
                    default:
                        break;
                }

            }

            for(Future<RandomAccessibleInterval<T>> f : futures) {
                RandomAccessibleInterval<T> result = f.get();
                stack.add(result);
            }
        } catch (InterruptedException e) {
            return null;
        } catch (ExecutionException e) {
            return null;
        } finally {
            exes.shutdown();
        }
        return Views.stack(stack);


    }

    private Callable<RandomAccessibleInterval<T>> getMax() {
        if(Thread.currentThread().isInterrupted()) {
            return null;
        }
        return new Callable<RandomAccessibleInterval<T>>() {

            @Override
            public RandomAccessibleInterval<T> call() throws Exception {
                return  KNIPGateway.ops().pixelfeature().max(img, (int)minSigma);
//                return null;
            }
        };
    }
}
