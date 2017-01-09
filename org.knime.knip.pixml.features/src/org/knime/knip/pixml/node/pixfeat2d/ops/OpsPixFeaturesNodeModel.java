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
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.node.ValueToCellNodeModel;
import org.knime.knip.pixml.node.pixfeat2d.ops.FeatureCalculator.Feature;

import net.imagej.ImgPlus;
import net.imagej.ImgPlusMetadata;
import net.imagej.axis.Axes;
import net.imagej.axis.CalibratedAxis;
import net.imagej.axis.DefaultLinearAxis;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

/**
 *
 * @author eike
 */
public class OpsPixFeaturesNodeModel<T extends RealType<T>> extends
ValueToCellNodeModel<ImgPlusValue<T>, ImgPlusCell<FloatType>> {

    final static SettingsModelStringArray createFeatureListModel() {
        // default selection
        String[] def = new String[5];
        for (int i = 0; i < def.length; i++) {
            def[i] = FeatureCalculator.getAvailableFeature(i);
        }
        return new SettingsModelStringArray("feature_list", def);
    }

    final static SettingsModelInteger createMinSigmaModel() {
        return new SettingsModelInteger("min_sigma", 1);
    }

    final static SettingsModelInteger createMaxSigmaModel() {
        return new SettingsModelInteger("max_sigma", 16);
    }

    final static SettingsModelString createFeatDimLabelModel() {
        return new SettingsModelString("feature_dim_label", "F");
    }

    private SettingsModelStringArray m_smFeatureList = createFeatureListModel();

    private SettingsModelInteger m_smMinSigma = createMinSigmaModel();

    private SettingsModelInteger m_smMaxSigma = createMaxSigmaModel();

    private SettingsModelString m_smFeatDimLabel = createFeatDimLabelModel();

    private boolean[] m_enabledFeatures;
    private Feature[] enabledFeatures;

    private ImgPlusCellFactory m_imgCellFactory;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addSettingsModels(final List<SettingsModel> settingsModels) {
        settingsModels.add(m_smFeatureList);
//        settingsModels.add(m_smMembraneThickness);
//        settingsModels.add(m_smMembranePatchSize);
        settingsModels.add(m_smMinSigma);
        settingsModels.add(m_smMaxSigma);
//        settingsModels.add(m_smMultiThreaded);
        settingsModels.add(m_smFeatDimLabel);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void prepareExecute(final ExecutionContext exec) {
        m_enabledFeatures = new boolean[FeatureCalculator.getAvailableFeatures().length];
        String[] selected = m_smFeatureList.getStringArrayValue();
        enabledFeatures = new Feature[selected.length];
//        int j = 0;
        for(int i = 0; i < selected.length; i++) {
            enabledFeatures[i] = Feature.valueOf(selected[i]);
        }
//        for (int i = 0; i < FeatureCalculator.getAvailableFeatures().length
//                && j < selected.length; i++) {
//            if (FeatureCalculator.getAvailableFeature(i).equals(selected[j])) {
//                m_enabledFeatures[i] = true;
//                j++;
//            }
//        }
        m_imgCellFactory = new ImgPlusCellFactory(exec);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ImgPlusCell<FloatType> compute(final ImgPlusValue<T> cellValue) throws Exception {
        ImgPlus<T> img = cellValue.getImgPlus();

        // create feature stack
        if (img.numDimensions() > 2) {
            throw new IllegalArgumentException("Only 2D images supported, yet!");
        }

//        ImagePlus ip = new ImagePlus();
//        new ImgToIJ(2).compute(img, ip);

        FeatureCalculator<T> opsFC = new FeatureCalculator<T>(img);

//        FeatureStack ijFeatureStack = new FeatureStack(ip);

        // set parameters
//        ijFeatureStack.setEnabledFeatures(m_enabledFeatures);
        opsFC.setSelectedFeatures(enabledFeatures);
//        ijFeatureStack
//                .setMembranePatchSize(m_smMembranePatchSize.getIntValue());
//        ijFeatureStack.setMembraneSize(m_smMembraneThickness.getIntValue());
//        ijFeatureStack.setMinimumSigma(m_smMinSigma.getIntValue());
//        ijFeatureStack.setMaximumSigma(m_smMaxSigma.getIntValue());
        opsFC.setMinSigma(m_smMinSigma.getIntValue());
        opsFC.setMaxSigma(m_smMaxSigma.getIntValue());

        // caluclate all features
//        if (m_smMultiThreaded.getBooleanValue()) {
//            ijFeatureStack.updateFeaturesMT();
//        } else {
//            ijFeatureStack.updateFeaturesST();
//        }

        // convert image stack to img

        RandomAccessibleInterval<T> out = opsFC.compute();
//        Img<FloatType> res =
//                new ArrayImgFactory<FloatType>().create(
//                        new long[]{img.dimension(0), img.dimension(1),
//                                ijFeatureStack.getSize()}, new FloatType());
//        Cursor<FloatType> resCur = res.cursor();
//        for (int i = 0; i < ijFeatureStack.getSize(); i++) {
//            for (int j = 0; j < img.size(); j++) {
//                resCur.fwd();
//                ImageProcessor tmp = ijFeatureStack.getProcessor(i + 1);
//                resCur.get().set(tmp.get(j));
//            }
//        }

        CalibratedAxis[] axes =
                new CalibratedAxis[]{
                        new DefaultLinearAxis(Axes.X),
                        new DefaultLinearAxis(Axes.Y),
                        new DefaultLinearAxis(Axes.get(m_smFeatDimLabel
                                .getStringValue()))};
        ImgPlusMetadata metadata = cellValue.getMetadata();
        for (int i = 0; i < axes.length; i++) {
            metadata.setAxis(axes[i], i);
        }

        return m_imgCellFactory
                .createCell(new ImgPlus<FloatType>((Img<FloatType>)out, metadata));
    }

}
