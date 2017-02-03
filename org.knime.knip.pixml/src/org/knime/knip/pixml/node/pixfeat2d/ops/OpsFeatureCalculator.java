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
import java.util.concurrent.Future;

import org.knime.knip.core.KNIPGateway;

import net.imagej.ImgPlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;

/**
 * This class calculates the features on a given image. It's required to set all parameters properly, otherwise default
 * values will be used.
 *
 * @author Eike Heinz, University of Konstanz
 * @param <T> type
 */
public class OpsFeatureCalculator<T extends RealType<T>> {

    private static String[] s_availableFeatures = new String[]{Feature.BILATERAL.getText(),
            Feature.DIFFERENCE_OF_GAUSSIAN.getText(), Feature.ENTROPY.getText(), Feature.GAUSSIAN_BLUR.getText(),
            Feature.HESSIAN.getText(), Feature.KUWAHARA.getText(), Feature.LAPLACIAN_OF_GAUSSIAN.getText(),
            Feature.MEMBRANE_PROJECTIONS.getText(), Feature.MAX.getText(), Feature.MEAN.getText(),
            Feature.MEDIAN.getText(), Feature.MIN.getText(), Feature.NEIGHBORS.getText(), Feature.SOBEL.getText(),
            Feature.STRUCTURE_TENSOR_EIGENVALUES.getText(), Feature.VARIANCE.getText()};

    /**
     * Enum containing all available features. Each feature has a string containing the displayable name.
     *
     * @author Eike Heinz, University of Konstanz
     */
    @SuppressWarnings("javadoc")
    public enum Feature {
        BILATERAL("Bilateral"), DIFFERENCE_OF_GAUSSIAN("Difference of Gaussians"), ENTROPY("Entropy"),
        GAUSSIAN_BLUR("Gaussian blur"), HESSIAN("Hessian"), KUWAHARA("Kuwahra"),
        LAPLACIAN_OF_GAUSSIAN("Laplacian of Gaussian"), MEMBRANE_PROJECTIONS("Membrane Projections"), MAX("Max"),
        MEAN("Mean"), MEDIAN("Median"), MIN("Min"), NEIGHBORS("Neighbors"), SOBEL("Sobel"),
        STRUCTURE_TENSOR_EIGENVALUES("Structure Tensor Eigenvalues"), VARIANCE("Variance");

        private final String name;

        private Feature(final String feat) {
            this.name = feat;
        }

        /**
         * @return name of feature
         */
        public String getText() {
            return this.name;
        }

        /**
         * Converts a name string into the correct feature.
         *
         * @param text of feature
         * @return corresponding feature enum
         */
        public static Feature fromString(final String text) {
            if (text != null) {
                for (Feature b : Feature.values()) {
                    if (text.equalsIgnoreCase(b.name)) {
                        return b;
                    }
                }
            }
            throw new IllegalArgumentException("no feature with this name");
        }
    }

    private Feature[] m_selectedFeatures;

    private Img<T> m_img;

    private int m_membraneSize = 1;

    private int m_membranePatchSize = 19;

    private float m_minSigma = 1.0f;

    private float m_maxSigma = 16.0f;

    private ExecutorService m_executor;

    /**
     * constructor
     *
     * @param input image of which features are calculated
     * @param exec {@link ExecutorService} that is reseted on canceling the node
     */
    public OpsFeatureCalculator(final ImgPlus<T> input, final ExecutorService exec) {
        m_img = input;
        m_executor = exec;
    }

    /**
     * Calculates all selected features on a given image.
     *
     * @return stack of features
     */
    public List<RandomAccessibleInterval<T>> compute() {
        if (Thread.currentThread().isInterrupted()) {
            return null;
        }

        List<RandomAccessibleInterval<T>> stack = new ArrayList<>();

        ArrayList<Future<RandomAccessibleInterval<T>>> futures = new ArrayList<>();

        // TODO check entropy, sobel, hessian, structure tensor, membrane Projections

        try {

            for (Feature feature : m_selectedFeatures) {
                switch (feature) {
                    case BILATERAL:
                        futures.add(m_executor.submit(getBilateral()));
                        break;
                    case DIFFERENCE_OF_GAUSSIAN:
                        futures.add(m_executor.submit(getDifferenceOfGaussian()));
                        break;
                    case ENTROPY:
                        futures.add(m_executor.submit(getEntropy()));
                        break;
                    case GAUSSIAN_BLUR:
                        futures.add(m_executor.submit(getGaussian()));
                        break;
                    case HESSIAN:
                        futures.add(m_executor.submit(getHessian()));
                        break;
                    case KUWAHARA:
                        futures.add(m_executor.submit(getKuwahara()));
                        break;
                    case LAPLACIAN_OF_GAUSSIAN:
                        futures.add(m_executor.submit(getLoG()));
                        break;
                    case MAX:
                        futures.add(m_executor.submit(getMax()));
                        break;
                    case MEAN:
                        futures.add(m_executor.submit(getMean()));
                        break;
                    case MEDIAN:
                        futures.add(m_executor.submit(getMedian()));
                        break;
                    case MEMBRANE_PROJECTIONS:
                        RandomAccessibleInterval<T> membrane = KNIPGateway.ops().pixelfeature()
                                .membraneProjections(m_img, m_membraneSize, m_membranePatchSize);
                        stack.add(membrane);
                        //                        futures.add(m_executor.submit(getMembraneProjections()));
                        break;
                    case MIN:
                        futures.add(m_executor.submit(getMin()));
                        break;
                    case NEIGHBORS:
                        futures.add(m_executor.submit(getNeighbors()));
                        break;
                    case SOBEL:
                        //                        RandomAccessibleInterval<T> manual = KNIPGateway.ops().pixelfeature().manualSobelFilter(m_img);
                        //                        ArrayImg<DoubleType, DoubleArray> test = KNIPGateway.ops().math().multiply((ArrayImg<DoubleType, DoubleArray>)manual, 0.125d);
                        RandomAccessibleInterval<T> separated = KNIPGateway.ops().filter().sobel(m_img);
                        //                        RandomAccessibleInterval<T> sobelFeature =
                        //                                KNIPGateway.ops().pixelfeature().sobel(m_img, m_minSigma, m_maxSigma);
                        RandomAccessibleInterval<T> sobelFeature = KNIPGateway.ops().pixelfeature().manualSobel(m_img);
//                        stack.add(getTestSobel());
                        stack.add(separated);
                        stack.add(sobelFeature);
//                        stack.add(derivative);
                        //                        stack.add((RandomAccessibleInterval<T>)test);
                        //                        futures.add(m_executor.submit(getSobel()));
                        break;
                    case STRUCTURE_TENSOR_EIGENVALUES:
                        futures.add(m_executor.submit(getStructureTensorEigenvalues()));
                        break;
                    case VARIANCE:
                        futures.add(m_executor.submit(getVariance()));
                        break;
                    default:
                        throw new IllegalArgumentException("Feature is not implemented");
                }
            }

            for (Future<RandomAccessibleInterval<T>> f : futures) {
                stack.add(f.get());
            }
        } catch (InterruptedException e) {
            return null;
        } catch (ExecutionException e) {
            return null;
        } finally {
            m_executor.shutdown();
        }
        return stack;
    }


    // -- Bilateral --
    private Callable<RandomAccessibleInterval<T>> getBilateral() {
        if (Thread.currentThread().isInterrupted()) {
            return null;
        }
        return new Callable<RandomAccessibleInterval<T>>() {

            @Override
            public RandomAccessibleInterval<T> call() throws Exception {
                return KNIPGateway.ops().pixelfeature().bilateral(m_img);
            }
        };
    }

    // -- Difference of Gaussian --
    private Callable<RandomAccessibleInterval<T>> getDifferenceOfGaussian() {
        if (Thread.currentThread().isInterrupted()) {
            return null;
        }
        return new Callable<RandomAccessibleInterval<T>>() {

            @Override
            public RandomAccessibleInterval<T> call() throws Exception {
                return KNIPGateway.ops().pixelfeature().doG(m_img, m_minSigma, m_maxSigma);
            }
        };
    }

    // -- Entropy --
    private Callable<RandomAccessibleInterval<T>> getEntropy() {
        if (Thread.currentThread().isInterrupted()) {
            return null;
        }
        return new Callable<RandomAccessibleInterval<T>>() {

            @Override
            public RandomAccessibleInterval<T> call() throws Exception {
                return KNIPGateway.ops().pixelfeature().entropy(m_img, m_minSigma, m_maxSigma, 5);
            }
        };
    }

    // -- Gaussian --
    private Callable<RandomAccessibleInterval<T>> getGaussian() {
        if (Thread.currentThread().isInterrupted()) {
            return null;
        }
        return new Callable<RandomAccessibleInterval<T>>() {

            @Override
            public RandomAccessibleInterval<T> call() throws Exception {
                return KNIPGateway.ops().pixelfeature().gaussian(m_img, m_minSigma, m_maxSigma);
            }
        };
    }

    // -- Hessian --
    private Callable<RandomAccessibleInterval<T>> getHessian() {
        if (Thread.currentThread().isInterrupted()) {
            return null;
        }
        return new Callable<RandomAccessibleInterval<T>>() {

            @Override
            public RandomAccessibleInterval<T> call() throws Exception {
                return KNIPGateway.ops().pixelfeature().hessian(m_img, m_minSigma, m_maxSigma);
            }
        };
    }

    // -- Kuwahara --
    private Callable<RandomAccessibleInterval<T>> getKuwahara() {
        if (Thread.currentThread().isInterrupted()) {
            return null;
        }
        return new Callable<RandomAccessibleInterval<T>>() {

            @Override
            public RandomAccessibleInterval<T> call() throws Exception {
                return KNIPGateway.ops().pixelfeature().kuwahara(m_img, (int)m_maxSigma);
            }
        };
    }

    // -- Laplacian of Gaussian --
    private Callable<RandomAccessibleInterval<T>> getLoG() {
        if (Thread.currentThread().isInterrupted()) {
            return null;
        }
        return new Callable<RandomAccessibleInterval<T>>() {

            @Override
            public RandomAccessibleInterval<T> call() throws Exception {
                return KNIPGateway.ops().pixelfeature().loG(m_img, m_minSigma, m_maxSigma);
            }
        };
    }

    // -- Max --
    private Callable<RandomAccessibleInterval<T>> getMax() {
        if (Thread.currentThread().isInterrupted()) {
            return null;
        }
        return new Callable<RandomAccessibleInterval<T>>() {

            @Override
            public RandomAccessibleInterval<T> call() throws Exception {
                return KNIPGateway.ops().pixelfeature().max(m_img, (int)m_maxSigma);
            }
        };
    }

    // -- Mean --
    private Callable<RandomAccessibleInterval<T>> getMean() {
        if (Thread.currentThread().isInterrupted()) {
            return null;
        }
        return new Callable<RandomAccessibleInterval<T>>() {

            @Override
            public RandomAccessibleInterval<T> call() throws Exception {
                return KNIPGateway.ops().pixelfeature().mean(m_img, (int)m_maxSigma);
            }
        };
    }

    // -- Median --
    private Callable<RandomAccessibleInterval<T>> getMedian() {
        if (Thread.currentThread().isInterrupted()) {
            return null;
        }
        return new Callable<RandomAccessibleInterval<T>>() {

            @Override
            public RandomAccessibleInterval<T> call() throws Exception {
                return KNIPGateway.ops().pixelfeature().median(m_img, (int)m_maxSigma);
            }
        };
    }

    // -- Membrane Projections --
    private Callable<RandomAccessibleInterval<T>> getMembraneProjections() {
        if (Thread.currentThread().isInterrupted()) {
            return null;
        }
        return new Callable<RandomAccessibleInterval<T>>() {

            @Override
            public RandomAccessibleInterval<T> call() throws Exception {
                return KNIPGateway.ops().pixelfeature().membraneProjections(m_img, m_membraneSize, m_membranePatchSize);
            }
        };
    }

    // -- Min --
    private Callable<RandomAccessibleInterval<T>> getMin() {
        if (Thread.currentThread().isInterrupted()) {
            return null;
        }
        return new Callable<RandomAccessibleInterval<T>>() {

            @Override
            public RandomAccessibleInterval<T> call() throws Exception {
                return KNIPGateway.ops().pixelfeature().min(m_img, (int)m_maxSigma);
            }
        };
    }

    // -- Neighbors --
    private Callable<RandomAccessibleInterval<T>> getNeighbors() {
        if (Thread.currentThread().isInterrupted()) {
            return null;
        }
        return new Callable<RandomAccessibleInterval<T>>() {

            @Override
            public RandomAccessibleInterval<T> call() throws Exception {
                return KNIPGateway.ops().pixelfeature().neighbors(m_img, (int)m_minSigma, (int)m_maxSigma);
            }
        };
    }

    // -- Sobel --
    private Callable<RandomAccessibleInterval<T>> getSobel() {
        if (Thread.currentThread().isInterrupted()) {
            return null;
        }
        return new Callable<RandomAccessibleInterval<T>>() {

            @Override
            public RandomAccessibleInterval<T> call() throws Exception {
                return KNIPGateway.ops().pixelfeature().sobel(m_img, m_minSigma, m_maxSigma);
            }
        };
    }

    // -- Structure Tensor Eigenvalues --
    private Callable<RandomAccessibleInterval<T>> getStructureTensorEigenvalues() {
        if (Thread.currentThread().isInterrupted()) {
            return null;
        }
        return new Callable<RandomAccessibleInterval<T>>() {

            @Override
            public RandomAccessibleInterval<T> call() throws Exception {
                return KNIPGateway.ops().pixelfeature().structureTensor(m_img, m_minSigma, m_maxSigma);

            }
        };
    }

    // -- Variance --
    private Callable<RandomAccessibleInterval<T>> getVariance() {
        if (Thread.currentThread().isInterrupted()) {
            return null;
        }
        return new Callable<RandomAccessibleInterval<T>>() {

            @Override
            public RandomAccessibleInterval<T> call() throws Exception {
                return KNIPGateway.ops().pixelfeature().variance(m_img, (int)m_maxSigma);
            }
        };
    }

    // -- getter and setter methods --

    /**
     * @param index
     * @return feature at index
     */
    public static String getAvailableFeature(final int index) {
        return s_availableFeatures[index];
    }

    /**
     * @return string array of implemented features
     */
    public static String[] getAvailableFeatures() {
        return s_availableFeatures;
    }

    /**
     * Enables selected features.
     *
     * @param features to be enabled
     */
    public void setSelectedFeatures(final Feature[] features) {
        m_selectedFeatures = features;
    }

    /**
     * @return selected features
     */
    public Feature[] getSelectedFeatures() {
        return m_selectedFeatures;
    }

    /**
     * @return the membraneSize
     */
    public int getMembraneSize() {
        return m_membraneSize;
    }

    /**
     * @param membraneSize the membraneSize to set
     */
    public void setMembraneSize(final int membraneSize) {
        this.m_membraneSize = membraneSize;
    }

    /**
     * @return the membranePatchSize
     */
    public int getMembranePatchSize() {
        return m_membranePatchSize;
    }

    /**
     * @param membranePatchSize the membranePatchSize to set
     */
    public void setMembranePatchSize(final int membranePatchSize) {
        this.m_membranePatchSize = membranePatchSize;
    }

    /**
     * Sets the value of minSigma.
     *
     * @param sigma
     */
    public void setMinSigma(final float sigma) {
        m_minSigma = sigma;
    }

    /**
     * @return minSigma
     */
    public float getMinSigma() {
        return m_minSigma;
    }

    /**
     * Sets the value of maxSigma.
     *
     * @param sigma
     */
    public void setMaxSigma(final float sigma) {
        m_maxSigma = sigma;
    }

    /**
     * @return maxSigma
     */
    public float getMaxSigma() {
        return m_maxSigma;
    }

}
