## What's Fixed

### 🐛 Semester End Date Bug
When you shortened a semester's end date, the app still tracked attendance for class occurrences that fell after the new end date. Those extra ghost classes are now correctly removed when you save the updated end date — only classes within the new date range remain.

> Already-marked attendance (Present / Absent / Cancelled) is **never** affected — only future unmarked classes beyond the new end date are removed.

---

**Full changelog:** https://github.com/shuaib-07/iAttend/commits/main
