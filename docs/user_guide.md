# User manual

## General structure of your flashcard collection

When you open the HyperCampus app, you should see the (initially empty) list of your _courses_, which are collections of cards that deal with the same topic. You can choose an icon (for example an emoji, or just a letter), a name and how many new cards you would like to study per day. I would recommend to study between 5 and 20 new cards for each course per day, but note that obviously you have to add some cards first before you can study any.

Courses consist of _lessons_, and the lessons contain your cards. New cards are added to your studies in the order that they are displayed in the app, progressing from the first to the last lesson. For each card, you can specify the contents of the "question" side and the "answer" side.

## Reviewing your cards

You can enter your daily review by pressing the floating button with the HyperCampus icon. First, new cards are displayed to you for studying. Then, cards due for review are shown and you should try to recall the answers on them. When you think you know the answer, or decide that you don't know it any more, reveal the answer side and grade your performance. First, indicate simply whether or not you knew the answer. Then, indicate how familiar you feel with that item, using the scale from purple (completely unfamiliar) to yellow (very familiar). Try to assess nuances in your performance. For example, if you knew it, but it took you a long time, tap "right" first and then towards the purple end. If you forgot it, but it was on the tip of your tongue, tap a "wrong" first and then towards the yellow end. You can undo the reviews from the current session one by one by using the system back button.

Apart from a complete review, you can choose to study a course or a lesson in isolation by tapping on the button indicating how many cards are due in the respective course or lesson. Note that new cards are only displayed when reviewing a course (i.e., not for a specific lesson). You should review all due cards every day as due cards will pile up when you don't review them, but the algorithm is adapted to work even if you review a card later than you should. You can also select some courses or lessons and tap on the (now black) HyperCampus button to review due cards from these courses or lessons only.

Sometimes you might want to review all your cards in specific courses or lessons, for example right before an exam. For this scenario, HyperCampus offers a "Full review" option in the selection menu to review all cards from the selected courses or lessons independent of whether they are due or not. The fact that you will review some cards prematurely will be accounted for by the HyperCampus algorithm.

You can also exclude certain cards from all reviews except full reviews. To do so, select the card(s) and choose "Disable" from the selection menu.

## Settings

In the settings, you can choose which spaced repetition algorithm to use. Of course I recommend the native HyperCampus algorithm, but as it is still experimental, I will allow you to use [SM-2](#the-supermemo-2-algorithm), on which Anki is based, as well. Note that when changing the algorithm, it will take some time to adapt it to your data, depending on how long you have used the other algorithm. For this reason, changing the algorithm frequently is not encouraged.

You can also set the _retention index_, which determines how much of the content learnt you want to keep in memory during any time. The default is 90%, meaning that the algorithms schedule cards such that you should be able to remember around 90% of the contents. Setting it higher is not recommended as it increases the workload without much benefit. If you are happy with a lower retention rate, you can set the retention index to let's say 80% to reduce workload. In fact, a lower retention rate leads to a much more effective acquisition; but this effect only works up to a retention index of 40%, after which the probability of recall is too low to allow memories to be acquired.

## Import/export

HyperCampus uses [Markdown](https://en.wikipedia.org/wiki/Markdown)-like files to make collections creatable and editable by humans on a computer, for example. If you download a `.md` file to your phone and open it with HyperCampus (by tapping on it; there is not yet a way to import files from inside the app), it will try to parse it as follows:
```markdown
# [A] MyCourse
This will create a course called "MyCourse" with symbol "A" (optional).
## [1] MyLesson
This creates a lesson called "MyLesson" inside MyCourse, with symbol "1".
Question   | Answer
-----------|--------
Hello?     | Hello!
What is ..?| 42
...        | ...
This inserts cards into MyLesson, with the question being in the left column and the answer in the right.
```
You can therefore simply create a collection on your computer, using any [Markdown editor](https://dillinger.io/) or just a [text editor](https://www.vim.org/). In markdown, course titles will be main headings, lesson titles become subheadings and cards are entries in a two-column table. Any text outside of headings and tables is ignored and can be used for additional remarks. Thus, you can store your HyperCampus collection as part of a bigger document with more information to enhance your studies! Note that a table header and an additional separating line (`---|---`) is required to create a table in markdown, so if your table doesn't start with it, it will be ignored.

To reference a media file in a card entry, use `![audio|media]{filename.jpg|mp3|...}`. The attribute inside `[]` **must** be set to either `image` or `audio`; the file will not be recognized based on file extension alone. If it is set to something other than `image` or `audio`, it will be ignored.

You can put an `.md` file together with the media files used in one folder and turn it into a `zip` file, which can be opened with HyperCampus. It will extract the media files and create the collection as specified in the `.md` file.

Note that if you open a `.md` or a `.zip` file with HyperCampus, it will always add new collections instead of updating existing ones. This is because there is simply no way to know if a card in the imported collection corresponds to an existing one or not. However, when you export your collection, you won't get a `.md` or `.zip` file. Instead, you will get a `.hcmd` or a `.hczip` file, depending on whether you selected to include media files or not. A `.hcmd` file is essentially a markdown file, but it is required to have one special column in each table that indicates the index of the respective card within the course, like
```markdown
n|Question   | Answer
-|-----------|--------
1|Hello?     | Hello!
2|What is ..?| 42
3|...        | ...
```
This way, if a `.hcmd` file is imported back into HyperCampus (for example after some editing on the computer), if you are importing a course with the same name as an existing one (the symbol does not matter), cards with the same index are going to be replaced without losing their scheduling information.

Similarly, a `.hczip` file is just a `.zip` file (simply rename it to extract its contents), but it has to contain a `.hcmd` file. In this case, media files with the same name will replace existing ones.
