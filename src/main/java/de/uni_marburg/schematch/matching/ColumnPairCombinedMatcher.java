package de.uni_marburg.schematch.matching;

import java.util.ArrayList;
import java.util.List;

import de.uni_marburg.schematch.matchtask.columnpair.ColumnPair;
import de.uni_marburg.schematch.data.metadata.Datatype;

public class ColumnPairCombinedMatcher {
	private final ColumnPairJaccardMatcher instanceMatcher=new ColumnPairJaccardMatcher();
	private final ColumnPairLabelMatcher labelMatcher =new ColumnPairLabelMatcher();

	private final double weightInstance = 0.4;
	private final double weightLabel = 0.4;
	private final double weightDatatype = 0.2;

	public double match(ColumnPair pair) {
        double instanceSim = instanceMatcher.match(pair);
        double labelSim = labelMatcher.match(pair);

		var type1 = pair.getSourceColumn().getDatatype();
		var type2 = pair.getTargetColumn().getDatatype();



		double datatypeSim;

		if (type1 == type2) {
			datatypeSim = 1.0;
		} else if (
				(type1 == Datatype.INTEGER && type2 == Datatype.FLOAT)
						||
						(type1 == Datatype.FLOAT && type2 == Datatype.INTEGER)
		) {
			datatypeSim = 0.5;
		} else {
			datatypeSim = 0.0;
		}

		double combinedSim =
				weightInstance * instanceSim
						+ weightLabel * labelSim
						+ weightDatatype * datatypeSim;
        return combinedSim;
	 }
	public List<Double> matchAll(List<ColumnPair> pairs){
        List<Double> results = new ArrayList<>();
        for(ColumnPair pair:pairs) {
        	double sim=match(pair);
        	results.add(sim);
        }
        return results;
	}

}
