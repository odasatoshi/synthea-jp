package org.mitre.synthea.world.concepts;

import java.util.List;

import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.helpers.SimpleYML;
import org.mitre.synthea.helpers.Utilities;
import org.mitre.synthea.world.agents.Person;

/**
 * Provides utilities for generating and managing names.
 */
public class Names {

  private static SimpleYML names = loadNames();

  private static SimpleYML loadNames() {
    String filename = "names.yml";
    try {
      String namesData = Utilities.readResource(filename);
      return new SimpleYML(namesData);
    } catch (Exception e) {
      System.err.println("ERROR: unable to load yml: " + filename);
      e.printStackTrace();
      throw new ExceptionInInitializerError(e);
    }
  }

  /**
   * Indicates whether numbers should be appended to generated names.
   * These numbers are used to indicate the patients as not real people.
   */
  public static final boolean appendNumbersToNames =
      Config.getAsBoolean("generate.append_numbers_to_person_names", false);

  /**
   * Name list in names.yml to draw all person names from, e.g. "japanese".
   * When set, it overrides the per-person language selection.
   */
  public static final String nameLocale = Config.get("generate.name_locale", "");

  /** True when names are written family-name-first, e.g. 山田 太郎. */
  public static final boolean familyNameFirst = "japanese".equals(nameLocale);

  /** Separator between a name and its kana (phonetic) reading in names.yml. */
  public static final char KANA_SEPARATOR = '|';

  /** Person attribute key: kana reading of the first name. */
  public static final String FIRST_NAME_KANA = "first_name_kana";
  /** Person attribute key: kana reading of the middle name. */
  public static final String MIDDLE_NAME_KANA = "middle_name_kana";
  /** Person attribute key: kana reading of the last name. */
  public static final String LAST_NAME_KANA = "last_name_kana";
  /** Person attribute key: kana reading of the full name. */
  public static final String NAME_KANA = "name_kana";
  /** Person attribute key: kana reading of the maiden name. */
  public static final String MAIDEN_NAME_KANA = "maiden_name_kana";

  /**
   * Split a names.yml entry into its written form and its kana reading.
   * "山田|ヤマダ" becomes {"山田", "ヤマダ"}; "Smith" becomes {"Smith", ""}.
   *
   * @param entry raw entry from names.yml
   * @return {name, reading}
   */
  private static String[] splitKana(String entry) {
    int i = entry.indexOf(KANA_SEPARATOR);
    String name = (i < 0) ? entry : entry.substring(0, i);
    String kana = (i < 0) ? "" : entry.substring(i + 1);
    if (appendNumbersToNames) {
      name = addHash(name);
      if (!kana.isEmpty()) {
        kana = addHash(kana);
      }
    }
    return new String[] {name, kana};
  }

  /**
   * Join the parts of a name in the order used by the configured locale.
   *
   * @param first given name
   * @param last family name
   * @return full name
   */
  public static String fullName(String first, String last) {
    return familyNameFirst ? last + " " + first : first + " " + last;
  }

  /**
   * Join the parts of a name in the order used by the configured locale.
   * Middle names are not a Japanese convention, but are kept if present.
   *
   * @param first given name
   * @param middle middle name, may be null
   * @param last family name
   * @return full name
   */
  public static String fullName(String first, String middle, String last) {
    if (middle == null || middle.isEmpty()) {
      return fullName(first, last);
    }
    return familyNameFirst ? last + " " + first + " " + middle
        : first + " " + middle + " " + last;
  }

  /**
   * Generate a first name appropriate for a given gender and language.
   * If `generate.append_numbers_to_person_names` == true,
   * then numbers will be appended automatically.
   * @param gender Gender of the name, "M" or "F"
   * @param language Origin language of the name, "english", "spanish"
   * @param person person to generate a name for.
   * @return First name.
   */
  public static String fakeFirstName(String gender, String language, Person person) {
    return firstNameWithKana(gender, language, person)[0];
  }

  /**
   * Generate a first name together with its kana reading.
   *
   * @param gender Gender of the name, "M" or "F"
   * @param language Origin language of the name, "english", "spanish"
   * @param person person to generate a name for.
   * @return {first name, kana reading}; the reading is "" if the list carries none.
   */
  @SuppressWarnings("unchecked")
  public static String[] firstNameWithKana(String gender, String language, Person person) {
    List<String> choices;
    if (!nameLocale.isEmpty()) {
      choices = (List<String>) names.get(nameLocale + "." + gender);
    } else if ("spanish".equalsIgnoreCase(language)) {
      choices = (List<String>) names.get("spanish." + gender);
    } else {
      choices = (List<String>) names.get("english." + gender);
    }

    // pick a random item from the list
    return splitKana(choices.get(person.randInt(choices.size())));
  }

  /**
   * Generate a surname appropriate for a given language.
   * If `generate.append_numbers_to_person_names` == true,
   * then numbers will be appended automatically.
   * @param language Origin language of the name, "english", "spanish"
   * @param person person to generate a name for.
   * @return Surname or Family Name.
   */
  public static String fakeLastName(String language, Person person) {
    return lastNameWithKana(language, person)[0];
  }

  /**
   * Generate a surname together with its kana reading.
   *
   * @param language Origin language of the name, "english", "spanish"
   * @param person person to generate a name for.
   * @return {surname, kana reading}; the reading is "" if the list carries none.
   */
  @SuppressWarnings("unchecked")
  public static String[] lastNameWithKana(String language, Person person) {
    List<String> choices;
    if (!nameLocale.isEmpty()) {
      choices = (List<String>) names.get(nameLocale + ".family");
    } else if ("spanish".equalsIgnoreCase(language)) {
      choices = (List<String>) names.get("spanish.family");
    } else {
      choices = (List<String>) names.get("english.family");
    }
    // pick a random item from the list
    return splitKana(choices.get(person.randInt(choices.size())));
  }

  /**
   * Generate a Street Address.
   * @param includeLine2 Whether or not the address should have a second line,
   *     which can take the form of an apartment, unit, or suite number.
   * @param person person to generate an address for.
   * @return First name.
   */
  @SuppressWarnings("unchecked")
  public static String fakeAddress(boolean includeLine2, Person person) {
    int number = person.randInt(1000) + 100;
    List<String> n = (List<String>)names.get("english.family");
    // for now just use family names as the street name.
    // could expand with a few more but probably not worth it
    String streetName = n.get(person.randInt(n.size()));
    List<String> a = (List<String>)names.get("street.type");
    String streetType = a.get(person.randInt(a.size()));

    if (includeLine2) {
      int addtlNum = person.randInt(100);
      List<String> s = (List<String>)names.get("street.secondary");
      String addtlType = s.get(person.randInt(s.size()));
      return number + " " + streetName + " " + streetType + " " + addtlType + " " + addtlNum;
    } else {
      return number + " " + streetName + " " + streetType;
    }
  }

  /**
   * Adds a 1- to 3-digit hashcode to the end of the name.
   * @param name Person's name
   * @return The name with a hash appended, ex "John123" or "Smith22"
   */
  public static String addHash(String name) {
    // note that this value should be deterministic
    // It cannot be a random number. It needs to be a hash value or something deterministic.
    // We do not want John10 and John52 -- we want all the Johns to have the SAME numbers. e.g. All
    // people named John become John52
    // Why? Because we do not know how using systems will index names. Say a user of an system
    // loaded with Synthea data wants to find all the people named John Smith. This will be easier
    // if John Smith always resolves to John52 Smith32 and not [John52 Smith32, John10 Smith22, ...]
    return name + Integer.toString(Math.abs(name.hashCode() % 1000));
  }
}