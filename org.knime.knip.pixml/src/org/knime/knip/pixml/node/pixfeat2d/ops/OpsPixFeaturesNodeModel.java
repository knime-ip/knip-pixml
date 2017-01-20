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
 *   12.12.2016 (eike): created
 */
package org.knime.knip.pixml.node.pixfeat2d.ops;

import java.util.List;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.node.ValueToCellNodeModel;
import org.knime.knip.core.KNIPGateway;
import org.knime.knip.pixml.node.pixfeat2d.ops.FeatureCalculator.Feature;

import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imagej.ImgPlusMetadata;
import net.imagej.axis.Axes;
import net.imagej.axis.CalibratedAxis;
import net.imagej.axis.DefaultLinearAxis;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

/**
 *
 * @author eike
 * @param <T> type
 */
public class OpsPixFeaturesNodeModel<T extends RealType<T>>
        extends ValueToCellNodeModel<ImgPlusValue<T>, ImgPlusCell<FloatType>> {

    final static SettingsModelStringArray createFeatureListModel() {
        // default selection
        String[] def = new String[5];
        for (int i = 0; i < def.length; i++) {
            def[i] = FeatureCalculator.getAvailableFeature(i);
        }
        return new SettingsModelStringArray("feature_list", def);
    }

    final static SettingsModelInteger createMembraneThicknessModel() {
        return new SettingsModelInteger("membrane_thickness", 1);
    }

    final static SettingsModelInteger createMembranePatchSizeModel() {
        return new SettingsModelInteger("membrane_patch_size", 19);
    }

    final static SettingsModelInteger createMinSigmaModel() {
        return new SettingsModelInteger("min_sigma", 1);
    }

    final static SettingsModelInteger createMaxSigmaModel() {
        return new SettingsModelInteger("max_sigma", 16);
    }

    final static SettingsModelBoolean createMultiThreadedModel() {
        return new SettingsModelBoolean("multi_threaded", true);
    }

    final static SettingsModelString createFeatDimLabelModel() {
        return new SettingsModelString("feature_dim_label", "F");
    }

    private SettingsModelStringArray m_smFeatureList = createFeatureListModel();

    private SettingsModelInteger m_smMembraneThickness = createMembraneThicknessModel();

    private SettingsModelInteger m_smMembranePatchSize = createMembranePatchSizeModel();

    private SettingsModelInteger m_smMinSigma = createMinSigmaModel();

    private SettingsModelInteger m_smMaxSigma = createMaxSigmaModel();

    private SettingsModelBoolean m_smMultiThreaded = createMultiThreadedModel();

    private SettingsModelString m_smFeatDimLabel = createFeatDimLabelModel();

    //    private boolean[] m_enabledFeatures;
    private Feature[] enabledFeatures;

    private ImgPlusCellFactory m_imgCellFactory;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addSettingsModels(final List<SettingsModel> settingsModels) {
        settingsModels.add(m_smFeatureList);
        settingsModels.add(m_smMembraneThickness);
        settingsModels.add(m_smMembranePatchSize);
        settingsModels.add(m_smMinSigma);
        settingsModels.add(m_smMaxSigma);
        settingsModels.add(m_smMultiThreaded);
        settingsModels.add(m_smFeatDimLabel);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void prepareExecute(final ExecutionContext exec) {
        //        m_enabledFeatures = new boolean[FeatureCalculator.getAvailableFeatures().length];
        String[] selected = m_smFeatureList.getStringArrayValue();
        enabledFeatures = new Feature[selected.length];

        for (int i = 0; i < selected.length; i++) {
            //            enabledFeatures[i] = Feature.valueOf(selected[i]);
            enabledFeatures[i] = Feature.fromString(selected[i]);
        }

        m_imgCellFactory = new ImgPlusCellFactory(exec);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ImgPlusCell<FloatType> compute(final ImgPlusValue<T> cellValue) throws Exception {
        OpService ops = KNIPGateway.ops();

        ImgPlus<T> img = cellValue.getImgPlus();
        //        ops.pixelfeature().dummyThread();

        // create feature stack
        if (img.numDimensions() > 2) {
            throw new IllegalArgumentException("Only 2D images supported, yet!");
        }

        FeatureCalculator<T> opsFC = new FeatureCalculator<T>(img, getExecutorService());

        // set parameters
        opsFC.setSelectedFeatures(enabledFeatures);
        // TODO set multithread, membrane patch, thickness
        opsFC.setMembraneSize(m_smMembraneThickness.getIntValue());
        opsFC.setMembranePatchSize(m_smMembranePatchSize.getIntValue());
        opsFC.setMultithreaded(m_smMultiThreaded.getBooleanValue());

        opsFC.setMinSigma(m_smMinSigma.getIntValue());
        opsFC.setMaxSigma(m_smMaxSigma.getIntValue());

        RandomAccessibleInterval<T> out = opsFC.compute();
//        System.out.println(out.dimension(0) + "|" + out.dimension(1) + "|" + out.dimension(2));
//        Cursor<T> outCursor = Views.iterable(out).cursor();
//        RandomAccess<T> outRA = out.randomAccess();
//        ImageJFunctions.show(out, "out");
//        Img<FloatType> res = new ArrayImgFactory<FloatType>()
//                .create(new long[]{out.dimension(0), out.dimension(1), out.dimension(2)}, new FloatType());
//        System.out.println(res.dimension(0) + "|" + res.dimension(1) + "|" + res.dimension(2));
//        RandomAccess<FloatType> resultRA = res.randomAccess();
//        Cursor<FloatType> resCur = res.cursor();

        // TODO convert RandomAccessibleInterval<T> out into Img<FloatType> res
        ImagePlus outImage = ImageJFunctions.wrap(out, "");
        Img<FloatType> outImg = ImagePlusAdapter.wrap(outImage);




        CalibratedAxis[] axes = new CalibratedAxis[]{new DefaultLinearAxis(Axes.X), new DefaultLinearAxis(Axes.Y),
                new DefaultLinearAxis(Axes.get(m_smFeatDimLabel.getStringValue()))};
        ImgPlusMetadata metadata = cellValue.getMetadata();
        for (int i = 0; i < axes.length; i++) {
            metadata.setAxis(axes[i], i);
        }

        // TODO create output imgplus

        return m_imgCellFactory.createCell(new ImgPlus<FloatType>( outImg, metadata));
        //        return m_imgCellFactory
        //                .createCell(new ImgPlus<FloatType>((Img<FloatType>)out, metadata));
    }

}
//        for (int i = 0; i < out.dimension(2); i++) {
//            // TODO slice result and copy values?
//            IntervalView<T> test = Views.hyperSlice(Views.hyperSlice(out, 3, 0), 2, i);
//            IntervalView<FloatType> resSlice = Views.hyperSlice(Views.hyperSlice(res, 3, 0), 2, i);
//            RandomAccess<FloatType> resSliceRA = resSlice.randomAccess();
//            Cursor<T> testCur = test.cursor();
//            while(testCur.hasNext()) {
//                testCur.next();
//                long[] position = new long[3];
//                testCur.localize(position);
//                position[2] = i;
//                resSliceRA.setPosition(testCur);
//                resSliceRA.get().setReal(testCur.get().getRealFloat());
////                outRA.setPosition(position);
////                outRA.get().setReal(testCur.get().getRealFloat());
//            }
//        }

//        ImageJFunctions.show(test, "test");
//        while(outCursor.hasNext()) {
//            outCursor.next();
//            resultRA.setPosition(outCursor);
//            resultRA.get().setReal(outCursor.get().getRealFloat());
//        }
//        while(resCur.hasNext()) {
//            resCur.next();
//            outRA.setPosition(resCur);
//            resCur.get().set(outRA.get().getRealFloat());
//        }
//        for (int i = 0; i < out.dimension(2); i++) {
//            for (int j = 0; j < img.size(); j++) {
//                resCur.fwd();
////                ImageProcessor tmp = ijFeatureStack.getProcessor(i + 1);
//                outRA.setPosition(resCur);
//                resCur.get().set(outRA.get().getRealFloat());
////                resCur.get().set(tmp.get(j));
//            }
//        }
