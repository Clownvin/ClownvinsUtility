package com.clownvin.util;


public class Util {
   public static String formatAsPercent(double value, int decimalPlaces) {
      return String.format("%."+decimalPlaces+"f", value * 100);
   }
}
