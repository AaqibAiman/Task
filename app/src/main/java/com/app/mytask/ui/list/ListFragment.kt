package com.app.mytask.ui.list

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.mytask.R
import com.app.mytask.model.UserResponse
import com.app.mytask.room.CommentDataBase
import com.app.mytask.room.Comments
import com.app.mytask.service.APIClient
import com.app.mytask.service.APIInterface
import com.app.mytask.ui.detailFragment.DetailsFragment
import kotlinx.android.synthetic.main.fragment_list.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class ListFragment : Fragment() , CustomAdapter.OnClickItem {

    var apiInterface: APIInterface? = null
    var nextId = 0
    var prevId = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View = inflater.inflate(
            R.layout.fragment_list, container,
            false
        )
        val activity = activity as Context

        apiInterface = APIClient.client?.create(APIInterface::class.java)
        gitHubRepositoryAPICall(nextId)
        //getting recyclerview from xml
        val recyclerView = view.findViewById(R.id.recyclerView) as RecyclerView
        val prevBtn = view.findViewById(R.id.prev) as Button
        val nextBtn = view.findViewById(R.id.next) as Button
        //adding a layoutmanager
        recyclerView.layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)

        prevBtn.setOnClickListener { gitHubRepositoryAPICall(nextId) }
        nextBtn.setOnClickListener { gitHubRepositoryAPICall(prevId) }
        return view
    }

    private fun gitHubRepositoryAPICall(id: Int) {
        val call: Call<List<UserResponse>> = apiInterface!!.doGetUserList(id)
        call.enqueue(object : Callback<List<UserResponse>> {
            override fun onResponse(
                call: Call<List<UserResponse>>,
                response: Response<List<UserResponse>>
            ) {
                if (response.isSuccessful) {

                    val userResponse = response.body()

                    if (!userResponse.isNullOrEmpty()) {
                        for (userData in userResponse) {
                            println("name ${userData.name}")
                        }
                        prevId = userResponse.first().id ?: 0
                        nextId = userResponse.last().id ?: 0
                    }

                    val adapter = activity?.let {
                        CustomAdapter(
                            userResponse as ArrayList<UserResponse>,
                            it, this@ListFragment
                        )
                    }
                    //now adding the adapter to recyclerview
                    recyclerView.adapter = adapter

                    Log.e("DATA123", response.body().toString())

                }
            }

            override fun onFailure(call: Call<List<UserResponse>>, t: Throwable?) {
                call.cancel()
            }
        })
    }

    private fun addComment(userResponse: UserResponse, comment: String) {
        var result = 0L
        if(comment.isBlank()) {
            putToast("Please add comment")
        }
        else {
            GlobalScope.launch {
             result = CommentDataBase.getDatabase(requireContext()).commentDao().insert(
                 Comments(
                     0,
                     userResponse.nodeId!!,
                     comment
                 )
             )
            if(result > 0) putToast("Comment added for ${userResponse.name}")
            }
        }
    }


    private fun putToast(message: String){
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    override fun clickItem(data: UserResponse) {

        val bundle = bundleOf(
            "userName" to data.name, "description" to data.description, "nodeId" to data.nodeId
        )
        val ft = fragmentManager?.beginTransaction()
        ft?.replace(R.id.nav_host_fragment, DetailsFragment(bundle))
        ft?.commit()
        ft?.addToBackStack(null)


    }

    override fun comments(data: UserResponse, comments: String) {
        addComment(data, comments)
    }
}