# constraintframelayout
ConstraintFrameLayout is a custom layout based on FrameLayout with some of functionality ConstraintLayout and more. You can do things with it that you can't do in ConstraintLayout. It's very lightweight because use only one measure and layout phase. And also because of this, it has certain limitations.

## Installation

Library is installed by putting aar file into libs folder:

```
module/libs (ex. app/libs)
```

and adding the aar dependency to your `build.gradle` file:
```groovy
dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.0-alpha05'
    implementation 'androidx.core:core-ktx:1.8.0'
    implementation "org.mini2Dx:gdx-math:1.9.13"
    implementation files("libs/constraintframelayout-1.4.0.aar")
}
```

## Phases
### Measure
##### Step 1
Measure via wrap content / match parent / 0dp (percent, weight, dimention to dimension.

##### Step 2: dimension ratio (optional)
Boundaries calculated in step 1 are use to measure via dimension ratio.

##### Step 3: factor (optional)
After steps 1 and 2, the final factor is applied.

## Layout
**Layout pass is after measure pass.**
#### You have a few options:
- **side to side** (f.ex. Left_toLeftOf, Top_toBottomOf, Center_toCenterOf, etc.)
- guides (f.ex. Guide_begin, Guide_end, Guide_percent, Guide_orientation)
- circle (Circle, CircleAngle, CircleRadius)

To layouting, by default is used bias constraint (**horizontalBias**, **verticalBias**).
Internal, layout use helper constraint, left, top, right, bottom:
- **left** of left side of parent,
- **top** of top side of parent,
- **right** of right side of parent,
- **bottom** of bottom side of parent

Default **horizontalBias** are set to 0.5 (between **left** and **right**)
Default **verticalBias** are set to 0.5 (between **top** and **bottom**)

To manipulate the left, top, right, bottom constraints are used **side to side** constrains.

Layout also include **guide** and **circle** constraints, like ConstraintLayout.

Layout also introduce something similar to helper in ConstraintLayout like virtual group: **Group** / **Flow**.
If you create group and add children (referenceIds), the children will used this group as parent and all whole calculations will depend of it.
Flow are used to arrange children like LinearLayout.

## Usage (examples)
https://github.com/mgolebiowski95/constraintframelayout/tree/master/app/src/main/res/layout

### Attributes
| Attribute | Format | Default |
|:---|:---:|:---:|
| ConstraintFrameLayout_measure_constraintWidth_percent | float |
| ConstraintFrameLayout_measure_constraintHeight_percent | float |
|||
| ConstraintFrameLayout_measure_constraintVertical_weight | float |
| ConstraintFrameLayout_measure_constraintHorizontal_weight | float |
|||
| ConstraintFrameLayout_measure_constraintWidth_toWidthOf | reference |
| ConstraintFrameLayout_measure_constraintWidth_toHeightOf | reference |
| ConstraintFrameLayout_measure_constraintHeight_toHeightOf | reference |
| ConstraintFrameLayout_measure_constraintHeight_toWidthOf | reference |
|||
| ConstraintFrameLayout_measure_constraintDimensionRatio | string |
|||
| ConstraintFrameLayout_measure_constraintWidthFactor | float | 1 |
| ConstraintFrameLayout_measure_constraintHeightFactor | float | 1 |
|||
| ConstraintFrameLayout_layout_constraintGuide_begin | dimension |
| ConstraintFrameLayout_layout_constraintGuide_end | dimension |
| ConstraintFrameLayout_layout_constraintGuide_percent | float |
|||
| ConstraintFrameLayout_layout_constraintGuide_orientation | horizontal/vertical |
|||
| ConstraintFrameLayout_layout_constraintLeft_toLeftOf | reference |
| ConstraintFrameLayout_layout_constraintLeft_toRightOf | reference |
| ConstraintFrameLayout_layout_constraintRight_toRightOf | reference |
| ConstraintFrameLayout_layout_constraintRight_toLeftOf | reference |
| ConstraintFrameLayout_layout_constraintTop_toTopOf | reference |
| ConstraintFrameLayout_layout_constraintTop_toBottomOf | reference |
| ConstraintFrameLayout_layout_constraintBottom_toBottomOf | reference |
| ConstraintFrameLayout_layout_constraintBottom_toTopOf | reference |
|||
| ConstraintFrameLayout_layout_constraintCenter_toCenterOf | reference |
|||
| ConstraintFrameLayout_layout_constraintHorizontal_bias | float | 0.5 |
| ConstraintFrameLayout_layout_constraintVertical_bias | float | 0.5 |
|||
| ConstraintFrameLayout_layout_constraintCircle | reference |
| ConstraintFrameLayout_layout_constraintCircleAngle | float | 0 |
| ConstraintFrameLayout_layout_constraintCircleRadius | dimension | 0 |

### Flow Attributes
| Attribute | Format |
|:---|:---:|
| Flow_orientation | horizontal/vertical |

### Virtual Layout Attributes
| Attribute | Format |
|:---|:---:|
| referenceIds | string |
