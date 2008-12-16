/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.web.generator.client;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.widgetideas.client.event.ChangeEvent;
import com.google.gwt.widgetideas.client.event.ChangeHandler;
import com.google.gwt.widgetideas.datepicker.client.DateBox;
import com.google.gwt.widgetideas.datepicker.client.TimePicker;
import com.google.zxing.web.generator.client.TimeZoneList.TimeZoneInfo;

import java.util.Date;

/**
 * A Generator for calendar events. Output is in VCal format.
 * 
 * @author Yohann Coppel
 */
public class CalendarEventGenerator implements GeneratorSource {
  public final static String[] FULL_DAY_ONLY_IDS = { "fullDayOnlyInfo1",
      "fullDayOnlyInfo2", "fullDayOnlyInfo3", "fullDayOnlyInfo4" };
  private final static long ONE_HOUR = 60 * 60 * 1000;

  Grid table = null;
  TextBox eventName = new TextBox();
  CheckBox fullDay = new CheckBox();
  DateBox datePicker1 = new DateBox();
  DateBox datePicker2 = new DateBox();
  TimePicker timePicker1 = new TimePicker(new Date(), DateTimeFormat
      .getFormat("a"), DateTimeFormat.getFormat("hh"), DateTimeFormat
      .getFormat("mm"), null);
  TimePicker timePicker2 = new TimePicker(new Date(), DateTimeFormat
      .getFormat("a"), DateTimeFormat.getFormat("hh"), DateTimeFormat
      .getFormat("mm"), null);
  CheckBox summerTime = new CheckBox();
  ListBox timeZones = new ListBox();
  Date timePicker1PreviousDate = null;

  public CalendarEventGenerator(final ChangeListener listener) {
    eventName.addStyleName(StylesDefs.INPUT_FIELD_REQUIRED);
    eventName.addChangeListener(listener);
    datePicker1.setAnimationEnabled(true);
    datePicker2.setAnimationEnabled(true);
    timePicker2
        .setDateTime(addMilliseconds(timePicker1.getDateTime(), ONE_HOUR));
    timePicker1PreviousDate = timePicker1.getDateTime();

    buildTimeZoneList();
    timeZones.setSelectedIndex(25);
    timePicker1.addChangeHandler(new ChangeHandler<Date>() {
      public void onChange(ChangeEvent<Date> event) {
        Date time = timePicker1PreviousDate;
        Date time1 = timePicker1.getDateTime();
        Date time2 = timePicker2.getDateTime();
        if (time2.after(time)) {
          // keep the same time difference if the interval is valid.
          long diff = timeDifference(time, time2);
          timePicker2.setDateTime(addMilliseconds(time1, diff));
        } else {
          // otherwise erase the end date and set it to startdate + one hour.
          timePicker2.setDateTime(addMilliseconds(time, ONE_HOUR));
        }
        // no need to call onChange for timePicker1, since it will be called
        // for timePicker2 when changes are made.
        // listener.onChange(timePicker1);
        timePicker1PreviousDate = time1;
      }
    });
    timePicker2.addChangeHandler(new ChangeHandler<Date>() {
      public void onChange(ChangeEvent<Date> event) {
        listener.onChange(timePicker2);
      }
    });
  }

  private void buildTimeZoneList() {
    for (TimeZoneInfo info : TimeZoneList.TIMEZONES) {
      timeZones.addItem(info.GMTRelative + " " + info.abreviation, ""
          + info.gmtDiff);
    }
  }

  public String getName() {
    return "Calendar event";
  }

  public Grid getWidget() {
    if (table != null) {
      return table;
    }
    datePicker1.getDatePicker().setSelectedDate(new Date());
    datePicker2.getDatePicker().setSelectedDate(new Date());
    table = new Grid(8, 2);

    table.setText(0, 0, "All day event");
    table.setWidget(0, 1, fullDay);

    table.setText(1, 0, "Event title");
    table.setWidget(1, 1, eventName);

    table.setText(2, 0, "Start date");
    table.setWidget(2, 1, datePicker1);

    table.setText(3, 0, "Time");
    table.setWidget(3, 1, timePicker1);

    table.setText(4, 0, "End date");
    table.setWidget(4, 1, datePicker2);

    table.setText(5, 0, "Time");
    table.setWidget(5, 1, timePicker2);

    table.setText(6, 0, "Time zone");
    table.setWidget(6, 1, timeZones);

    table.setText(7, 0, "Daylight savings");
    table.setWidget(7, 1, summerTime);

    table.getRowFormatter().getElement(3).setId(FULL_DAY_ONLY_IDS[0]);
    table.getRowFormatter().getElement(5).setId(FULL_DAY_ONLY_IDS[1]);
    table.getRowFormatter().getElement(6).setId(FULL_DAY_ONLY_IDS[2]);
    table.getRowFormatter().getElement(7).setId(FULL_DAY_ONLY_IDS[3]);

    fullDay.addClickListener(new ClickListener() {
      public void onClick(Widget sender) {
        CheckBox cb = (CheckBox) sender;
        setFullDay(cb.isChecked());
      }
    });

    return table;
  }

  private void setFullDay(boolean fullDay) {
    for (String s : FULL_DAY_ONLY_IDS) {
      Element element = DOM.getElementById(s);
      String style = "";
      if (fullDay) {
        style = "none";
      }
      DOM.setStyleAttribute(element, "display", style);
    }
  }

  public String getText() throws GeneratorException {
    String eventName = getEventNameField();
    String dates = getDateTimeFields();

    String output = "";
    output += "BEGIN:VEVENT\n";
    output += eventName;
    output += dates;
    output += "END:VEVENT\n";
    return output;
  }

  private String getEventNameField() throws GeneratorException {
    String inputName = eventName.getText();
    if (inputName.length() < 1) {
      throw new GeneratorException("Event name must be at least 1 character.");
    }
    if (inputName.contains("\n")) {
      throw new GeneratorException(
          "Event name should not contain \\n characters.");
    }
    return "SUMMARY:" + inputName + "\n";
  }

  private String getDateTimeFields() throws GeneratorException {
    if (fullDay.isChecked()) {
      return getFullDayDateFields();
    }
    return getDateTimeValues();
  }

  private String getFullDayDateFields() throws GeneratorException {
    Date date1 = datePicker1.getDatePicker().getSelectedDate();
    Date date2 = datePicker2.getDatePicker().getSelectedDate();
    if (null == date1 || null == date2) {
      throw new GeneratorException("Start and end dates must be set.");
    }
    if (date1.after(date2)) {
      throw new GeneratorException("Ending date is after starting date.");
    }
    DateTimeFormat isoFormatter = DateTimeFormat.getFormat("yyyyMMdd");
    String output = "";
    output += "DTSTART:" + isoFormatter.format(date1) + "\n";
    output += "DTEND:" + isoFormatter.format(date2) + "\n";
    return output;
  }

  private String getDateTimeValues() throws GeneratorException {
    Date date1 = datePicker1.getDatePicker().getSelectedDate();
    Date date2 = datePicker2.getDatePicker().getSelectedDate();
    Date time1 = timePicker1.getDateTime();
    Date time2 = timePicker2.getDateTime();
    if (null == date1 || null == date2 || null == time1 || null == time2) {
      throw new GeneratorException("Start and end dates/times must be set.");
    }
    String timezoneDelta = timeZones.getValue(timeZones.getSelectedIndex());
    long diffTimeZone = Long.parseLong(timezoneDelta);
    if (summerTime.isChecked()) {
      diffTimeZone += ONE_HOUR;
    }
    Date dateTime1 = addMilliseconds(mergeDateAndTime(date1, time1),
        -diffTimeZone);
    Date dateTime2 = addMilliseconds(mergeDateAndTime(date2, time2),
        -diffTimeZone);
    if (dateTime1.after(dateTime2)) {
      throw new GeneratorException("Ending date is after starting date.");
    }
    DateTimeFormat isoFormatter = DateTimeFormat
        .getFormat("yyyyMMdd'T'kkmmss'Z'");
    String output = "";
    output += "DTSTART:" + isoFormatter.format(dateTime1) + "\n";
    output += "DTEND:" + isoFormatter.format(dateTime2) + "\n";
    return output;
  }

  private Date mergeDateAndTime(Date date, Date time) {
    // Is that the only ugly way to do with GWT ? given that we don't
    // have java.util.Calendar for instance
    DateTimeFormat extractDate = DateTimeFormat.getFormat("yyyyMMdd");
    DateTimeFormat extractTime = DateTimeFormat.getFormat("kkmm");
    DateTimeFormat merger = DateTimeFormat.getFormat("yyyyMMddkkmmss");
    String d = extractDate.format(date);
    String t = extractTime.format(time) + "00";
    return merger.parse(d + t);
  }

  public void validate(Widget widget) throws GeneratorException {
    if (widget == eventName)
      getEventNameField();
    if (widget == datePicker1 || widget == timePicker1 || widget == datePicker2
        || widget == timePicker2)
      getDateTimeFields();
  }

  private static Date addMilliseconds(Date time1, long milliseconds) {
    return new Date(time1.getTime() + milliseconds);
  }

  private static long timeDifference(Date time1, Date time2) {
    return time2.getTime() - time1.getTime();
  }

  public void setFocus() {
    eventName.setFocus(true);
  }
}
