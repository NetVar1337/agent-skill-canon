> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/enma-lang/addons/time.md).

# Time

Registered with `register_addon_time(engine)`.

A timestamp is an `int64` count of **microseconds since the Unix epoch** (1970-01-01 00:00:00 UTC). Every calendar accessor interprets it in UTC.

## Reading the clock

```c
int64 now_us()          // microseconds since the epoch
int64 now_ms()          // milliseconds since the epoch
int64 now_ns()          // nanoseconds since the epoch
int64 unix_seconds()    // seconds since the epoch
int64 mono_us()         // monotonic counter — for deltas, not a date
```

Use `mono_us()` to measure elapsed time: it never jumps when the wall clock is adjusted.

```c
int64 t0 = mono_us();
do_work();
println(std::to_string(mono_us() - t0));   // microseconds spent
```

## Building a timestamp

```c
int64 from_ymd(int64 year, int64 month, int64 day)
int64 from_ymdhms(int64 year, int64 month, int64 day,
                  int64 hour, int64 minute, int64 second)
```

```c
int64 midnight = from_ymd(2024, 1, 15);                   // 00:00:00 UTC
int64 t        = from_ymdhms(2024, 6, 15, 12, 30, 45);
```

## Calendar accessors

```c
int64 year(int64 us)
int64 month(int64 us)          // 1..12
int64 day(int64 us)            // 1..31
int64 hour(int64 us)           // 0..23
int64 minute(int64 us)         // 0..59
int64 second(int64 us)         // 0..59
int64 day_of_week(int64 us)    // 0 = Sunday .. 6 = Saturday
int64 day_of_year(int64 us)    // 1..366
```

```c
int64 t = from_ymdhms(2024, 6, 15, 12, 30, 45);

year(t);          // 2024
month(t);         // 6
day(t);           // 15
hour(t);          // 12
day_of_week(t);   // 6 — a Saturday
day_of_year(t);   // 167
```

## Calendar queries

```c
bool  is_leap(int64 year)
int64 days_in_month(int64 year, int64 month)   // 0 for a month outside 1..12
```

```c
is_leap(2024);                // true
days_in_month(2024, 2);       // 29
days_in_month(2024, 13);      // 0
```

## ISO 8601

`iso_format` produces `YYYY-MM-DDTHH:MM:SS.ffffffZ`. `iso_parse` accepts that full form, `YYYY-MM-DDTHH:MM:SS`, or a bare `YYYY-MM-DD` — the fractional part and the trailing `Z` are both optional.

```c
std::string iso_format(int64 us)
int64 iso_parse(const char* text)
int64 iso_parse(const std::string& text)
```

```c
std::string s = iso_format(t);       // "2024-06-15T12:30:45.000000Z"
iso_parse(s) == t;                   // true — round-trips
hour(iso_parse("2024-06-15"));       // 0 — a bare date is midnight UTC
```

## Arithmetic

```c
int64 add_seconds(int64 us, int64 seconds)
int64 add_days(int64 us, int64 days)
int64 diff_us(int64 later, int64 earlier)
int64 diff_ms(int64 later, int64 earlier)
int64 diff_s(int64 later, int64 earlier)
```

```c
int64 later = add_seconds(t, 90);

day(add_days(t, 1));      // 16
diff_s(later, t);         // 90
diff_ms(later, t);        // 90000
diff_us(later, t);        // 90000000
```

## Sleep

```c
void sleep_ms(int64 milliseconds)
```

For finer granularity use `sleep_us` from the [thread addon](/perception/enma-lang/addons/thread.md).
