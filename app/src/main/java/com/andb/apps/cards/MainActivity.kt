package com.andb.apps.cards

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.andb.apps.cards.repository.CardRepo
import com.github.rongi.klaster.Klaster
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.card_item.view.*
import kotlinx.android.synthetic.main.drawer_bar_layout.*
import kotlinx.android.synthetic.main.drawer_bar_layout.view.*


class MainActivity : AppCompatActivity() {

    private val viewModel: MainActivityViewModel by lazy {
        ViewModelProviders.of(this).get(MainActivityViewModel::class.java)
    }

    private val cardAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder> by lazy { cardAdapter() }

    private val behavior by lazy {
        BottomSheetBehavior.from(drawerBottomSheet)
            .also {
                it.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(p0: View, p1: Int) {}
                    override fun onSlide(v: View, scroll: Float) {
                        v.bottomAppBar.getToolbarNavigationButton()?.rotation = (180 * scroll)
                    }
                })
            }
    }

    private val prefs by lazy { getSharedPreferences("Persist", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.colorPrimaryDark)
        setContentView(R.layout.activity_main)

        CardRepo.card.observe(this as LifecycleOwner, Observer {
            refreshBottomBar()
        })

        CardRepo.currentCard.value = prefs.getInt("currentCard", 0)

        addExpenseFAB.setOnClickListener {
            showAddExpenseFragment()
        }

        drawerCardRecycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = cardAdapter

        }

        refreshBottomBar()

        bottomAppBar.getToolbarNavigationButton()?.setOnClickListener {
            if (behavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                showBottomBar()
            } else {
                if (!behavior.isHideable) {
                    showBottomSheet()
                }
            }
        }
    }

    override fun onBackPressed() {
        when {
            supportFragmentManager.backStackEntryCount > 0 -> {
                supportFragmentManager.popBackStack()
                showBottomBar()
            }
            else -> super.onBackPressed()
        }
    }

    fun showAddExpenseFragment(editID: Int = -1) {
        val fragment = AddExpenseFragment()
        fragment.arguments = Bundle().also { it.putInt("expenseID", editID) }
        supportFragmentManager.beginTransaction()
            .add(R.id.addExpenseFrame, fragment)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .addToBackStack("addExpenseFragment").commit()

        hideBottomBar()
    }

    private fun refreshBottomBar() {
        showBottomBar()
        cardAdapter.notifyDataSetChanged()
    }

    private fun cardAdapter() = Klaster.get()
        .itemCount { CardRepo.cards.value?.size ?: 0 }
        .view(R.layout.card_item, layoutInflater)
        .bind { pos ->
            val card = CardRepo.cards.value?.get(pos) ?: return@bind
            itemView.apply {
                cardItemIcon.setImageResource(card.getDrawableID())
                cardItemName.text = card.name
                if (pos == CardRepo.currentCard.value) {
                    setBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimaryDark))
                } else {
                    addRipple()
                }
                setOnClickListener {
                    CardRepo.currentCard.value = pos
                    prefs.edit { putInt("currentCard", pos) }
                }
            }
        }
        .build()

    private fun hideBottomBar() {
        behavior.skipCollapsed = true
        behavior.isHideable = true
        behavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    fun showBottomBar() {
        behavior.skipCollapsed = false
        behavior.isHideable = false
        behavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun showBottomSheet() {
        behavior.skipCollapsed = false
        behavior.isHideable = false
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }
}

private fun View.addRipple() = with(TypedValue()) {
    context.theme.resolveAttribute(android.R.attr.selectableItemBackground, this, true)
    setBackgroundResource(resourceId)
}

private fun Toolbar.getToolbarNavigationButton(): ImageButton? {
    val size = childCount
    for (i in 0 until size) {
        val child = getChildAt(i)
        if (child is ImageButton) {
            if (child.drawable === navigationIcon) {
                return child
            }
        }
    }
    return null
}
