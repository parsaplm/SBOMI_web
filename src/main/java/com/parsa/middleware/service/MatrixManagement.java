package com.parsa.middleware.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.logging.Logger;

/**
 * 
 * @author(name = "Lukas Keul", company = "Parsa PLM GmbH")
 *
 */
public class MatrixManagement {
	/**
	 * Configure the absolute transformation matrix depending on the given
	 * parameters from the JSON file.
	 * 
	 * @param displayString
	 * 
	 * @return
	 */
	public static String calculateTransformationMatrix(String coordinates, String rotation, String displayString,
			Logger logger) {
		logger.info("Calculate the transformation matrix for " + displayString + "." + ",coordinates:"+ coordinates +", rotation"+ rotation);

		final Double[][] unitMatrix = new Double[][] { { 1.0, 0.0, 0.0, 0.0 }, { 0.0, 1.0, 0.0, 0.0 },
				{ 0.0, 0.0, 1.0, 0.0 }, { 0.0, 0.0, 0.0, 1.0 } };

		// Get the translation values
		final String[] coords = coordinates.split(";");
		final double x = Double.parseDouble(coords[0]);
		final double y = Double.parseDouble(coords[1]);
		final double z = Double.parseDouble(coords[2]);

		// Get the rotation values
		final String[] rotationAngles = rotation.split(";");
		final double alpha = Double.parseDouble(rotationAngles[2]); // Angle for R_x
		final double beta = Double.parseDouble(rotationAngles[1]); // Angle for R_y
		final double gamma = Double.parseDouble(rotationAngles[0]); // Angle for R_z

		Double[][] absoluteTransformationMatrix = translate(unitMatrix, x, y, z, logger);
		absoluteTransformationMatrix = rotate(absoluteTransformationMatrix, alpha, beta, gamma, logger);
		absoluteTransformationMatrix = imageSwap(absoluteTransformationMatrix, logger);

		String stringAbsoluteTransformationMatrix = "";

		// To set the separator to a dot instead of the local one
		final DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols();
		otherSymbols.setDecimalSeparator('.');

		final DecimalFormat df = new DecimalFormat("#.###", otherSymbols);

		for (final Double[] element : absoluteTransformationMatrix) {
			for (final double number : element) {
				stringAbsoluteTransformationMatrix = stringAbsoluteTransformationMatrix + df.format(number) + " ";
			}
		}

		logger.info(stringAbsoluteTransformationMatrix);
		return stringAbsoluteTransformationMatrix.trim();
	}

	/**
	 * Configure the absolute transformation matrix depending on the given
	 * parameters from the JSON file.
	 * 
	 * @param jsonObject
	 * 
	 * @return
	 */
	public static String calculateTransformationMatrix(String coordinates, String rotation, Logger logger) {
		logger.info("Calculate the transformation matrix.");

		final Double[][] unitMatrix = new Double[][] { { 1.0, 0.0, 0.0, 0.0 }, { 0.0, 1.0, 0.0, 0.0 },
				{ 0.0, 0.0, 1.0, 0.0 }, { 0.0, 0.0, 0.0, 1.0 } };

		// Get the translation values
		final String[] coords = coordinates.split(";");
		final double x = Double.parseDouble(coords[0]);
		final double y = Double.parseDouble(coords[1]);
		final double z = Double.parseDouble(coords[2]);

		// Get the rotation values
		final String[] rotationAngles = rotation.split(";");
		final double alpha = Double.parseDouble(rotationAngles[2]); // Angle for R_x
		final double beta = Double.parseDouble(rotationAngles[1]); // Angle for R_y
		final double gamma = Double.parseDouble(rotationAngles[0]); // Angle for R_z

		Double[][] absoluteTransformationMatrix = translate(unitMatrix, x, y, z, logger);
		absoluteTransformationMatrix = rotate(absoluteTransformationMatrix, alpha, beta, gamma, logger);
		absoluteTransformationMatrix = imageSwap(absoluteTransformationMatrix, logger);

		String stringAbsoluteTransformationMatrix = "";

		// To set the separator to a dot instead of the local one
		final DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols();
		otherSymbols.setDecimalSeparator('.');

		final DecimalFormat df = new DecimalFormat("#.###", otherSymbols);

		for (final Double[] element : absoluteTransformationMatrix) {
			for (final double number : element) {
				stringAbsoluteTransformationMatrix = stringAbsoluteTransformationMatrix + df.format(number) + " ";
			}
		}

		logger.info(stringAbsoluteTransformationMatrix);
		return stringAbsoluteTransformationMatrix.trim();
	}

	/**
	 * Compute the absolute rotation matrix with the given parameters.
	 * 
	 * @param matrix A matrix, in which the rotation will be put
	 * @param alpha  The angle for the x-axis
	 * @param beta   The angle for the y-axis
	 * @param gamma  The angle for the z-axis
	 * @return A 4x4 matrix with the rotation values
	 */
	private static Double[][] rotate(Double[][] matrix, double alpha, double beta, double gamma, Logger logger) {
		logger.info("Calculate the rotation of the transformation matrix.");

		final double cosAlpha = Math.cos(Math.toRadians(alpha));
		final double cosBeta = Math.cos(Math.toRadians(beta));
		final double cosGamma = Math.cos(Math.toRadians(gamma));
		final double sinAlpha = Math.sin(Math.toRadians(alpha));
		final double sinBeta = Math.sin(Math.toRadians(beta));
		final double sinGamma = Math.sin(Math.toRadians(gamma));

		// https://en.wikipedia.org/wiki/Rotation_matrix
		final Double[] firstRow = { round(cosAlpha * cosBeta, 3),
				round(cosAlpha * sinBeta * sinGamma - (sinAlpha * cosGamma), 3),
				round(cosAlpha * sinBeta * cosGamma + sinAlpha * sinGamma, 3), matrix[0][3] };
		final Double[] secondRow = { round(sinAlpha * cosBeta, 3),
				round(sinAlpha * sinBeta * sinGamma + cosAlpha * cosGamma, 3),
				round(sinAlpha * sinBeta * cosGamma - (cosAlpha * sinGamma), 3), matrix[1][3] };
		final Double[] thirdRow = { round(-sinBeta, 3), round(cosBeta * sinGamma, 3), round(cosBeta * cosGamma, 3),
				matrix[2][3] };

		final Double[][] rotationMatrix = new Double[][] { firstRow, secondRow, thirdRow, matrix[3] };

		return rotationMatrix;
	}

	/**
	 * Translate the matrix by the vector (x, y, z).
	 * 
	 * @param matrix A 4x4 matrix of doubles
	 * @param x      The translation of the x-axis
	 * @param y      The translation of the y-axis
	 * @param z      The translation of the z-axis
	 * @return
	 */
	private static Double[][] translate(Double[][] matrix, double x, double y, double z, Logger logger) {
		logger.info("Calculate the translation of the transformation matrix.");

		final Double[][] translationMatrix = matrix;
		translationMatrix[0][3] = round(x / 1000, 3);
		translationMatrix[1][3] = round(y / 1000, 3);
		translationMatrix[2][3] = round(z / 1000, 3);

		return translationMatrix;
	}

	/**
	 * Round the given value to the given decimal places.
	 * 
	 * @param value  Any double value.
	 * @param places The amount of decimal places after the comma
	 * @return
	 */
	private static double round(double value, int places) {

		if (places < 0) {
			throw new IllegalArgumentException();
		}

		BigDecimal bd = new BigDecimal(Double.toString(value));
		bd = bd.setScale(places, RoundingMode.HALF_UP);
		return bd.doubleValue();
	}

	/**
	 * Mirror the matrix on the diagonal.
	 * 
	 * @param matrix A 4x4 matrix of doubles
	 * @return The given matrix mirrored on the diagonal
	 */
	private static Double[][] imageSwap(Double[][] matrix, Logger logger) {
		logger.info("Mirror the matrix on the diagonal axis.");

		// traverse a matrix and swap
		// mat[i][j] with mat[j][i]
		for (int i = 0; i < matrix.length; i++) {
			for (int j = 0; j <= i; j++) {
				matrix[i][j] = matrix[i][j] + matrix[j][i] - (matrix[j][i] = matrix[i][j]);
			}
		}

		return matrix;
	}
}
