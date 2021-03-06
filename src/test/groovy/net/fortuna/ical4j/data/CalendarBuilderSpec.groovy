package net.fortuna.ical4j.data

import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.util.CompatibilityHints
import spock.lang.Specification

import java.nio.charset.Charset

/**
 * Created by fortuna on 4/07/2016.
 */
class CalendarBuilderSpec extends Specification {

    def 'test relaxed parsing'() {
        given: 'a calendar object string'
        String cal2 = getClass().getResource('test-relaxed-parsing.ics').text

        and: 'relaxed parsing is enabled'
        CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_UNFOLDING, true)

        when: 'the calendar string is parsed'
        System.out.println(cal2);
        InputStream stream = new ByteArrayInputStream(cal2.getBytes(Charset.forName("UTF-8")));
        CalendarBuilder builder = new CalendarBuilder();
        Calendar calendar = null;

        calendar = builder.build(stream);

        then: 'the result is as expected'
        calendar as String == cal2.replaceAll('\n', '\r\n')
    }
}
