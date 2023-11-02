package com.mrikso.apkrepacker.fragment

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.ybq.android.spinkit.style.CubeGrid
import com.google.android.material.button.MaterialButton
import com.mrikso.apkrepacker.R
import com.mrikso.apkrepacker.adapter.ErrorAdapter
import com.mrikso.apkrepacker.fragment.dialogs.bottomsheet.FileOptionsDialogFragment
import com.mrikso.apkrepacker.fragment.dialogs.bottomsheet.FullLogDialogFragment
import com.mrikso.apkrepacker.service.BuildService
import com.mrikso.apkrepacker.utils.*
import com.mrikso.apkrepacker.utils.common.DLog
import com.mrikso.apkrepacker.viewmodel.CompileFragmentViewModel
import java.io.File

class CompileFragment : Fragment(), ErrorAdapter.OnItemInteractionListener {

    private var mViewModel: CompileFragmentViewModel? = null
    private lateinit var mService: BuildService
    private val mAdapter = ErrorAdapter()
    private var mBound: Boolean = false
    private var mFinish: Boolean = false
    private var mErrorLogList: RecyclerView? = null
  //  private var mContext: Context? = null
    private var mProjectDir: String? = null
    private var mLayoutApkCompiling: LinearLayout? = null
    private var mLayoutApkCompiled: LinearLayout? = null
    private var mUninstallApp: MaterialButton? = null
    private var mInstallApp: MaterialButton? = null
    private var mCloseFragment: MaterialButton? = null
    private var mShowLog: MaterialButton? = null
    private var mCopyLog: MaterialButton? = null
    private var mProgressBar: ProgressBar? = null
    private var mProgressTip: TextView? = null
    private var mSavedFileMsg: AppCompatTextView? = null
    private var mImageError: AppCompatImageView? = null
    private var mCompiledTime: AppCompatTextView? = null

    companion object {
        @JvmStatic
        fun newInstance(param1: String?): CompileFragment {
            val fragment = CompileFragment()
            val args = Bundle()
            args.putString("project", param1)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mProjectDir = if (arguments != null) requireArguments().getString("project") else null
        retainInstance = true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("serviceRunning", mBound)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_compile, container, false)
        mViewModel =  ViewModelProvider(this).get(CompileFragmentViewModel::class.java)
        return view
    }

    override fun onViewCreated(view: View, bundle: Bundle?) {
        super.onViewCreated(view, bundle)
       // mContext = view.context
        mErrorLogList = view.findViewById(R.id.error_list)
        mLayoutApkCompiling = view.findViewById(R.id.layout_apk_compiling)
        mLayoutApkCompiled = view.findViewById(R.id.layout_apk_compiled)
        mUninstallApp = view.findViewById(R.id.btn_remove)
        mInstallApp = view.findViewById(R.id.btn_install)
        mCloseFragment = view.findViewById(R.id.btn_close)
        mShowLog = view.findViewById(R.id.btn_show_log)
        mCopyLog = view.findViewById(R.id.btn_copy_log)
        mProgressBar = view.findViewById(R.id.progressBar)
        mProgressTip = view.findViewById(R.id.progress_tip)
        mImageError = view.findViewById(R.id.image_error)
        mCompiledTime = view.findViewById(R.id.message_build_file_time)
        mSavedFileMsg = view.findViewById(R.id.message_build_file_saved)
        val cubeGrid = CubeGrid()
        cubeGrid.setBounds(0, 0, 100, 100)
        cubeGrid.color = ViewUtils.getThemeColor(requireContext(), R.attr.colorAccent)
        cubeGrid.alpha = 0
        mProgressBar!!.indeterminateDrawable = cubeGrid

        mErrorLogList?.apply{
            layoutManager = LinearLayoutManager(requireContext())
            adapter = mAdapter
        }
        if (bundle != null) {
            mBound = bundle.getBoolean("serviceRunning")
        }
        if(!mBound)
        bindLocalService()
        startBuildService()
    }

    private fun initList(string: String?){
        mErrorLogList?.let { ViewUtils.setVisibleOrGone(it, true) }
        mAdapter.apply {
            setItemInteractionListener(this@CompileFragment)
            updateMessage(string)
        }
    }

    fun builded(result: File?) {
      //  requireContext().unbindService(connection)
        //mBound = false
      //  stopBuildService()

        if (result != null) {
            //apk было собрано успешно
            val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.about_card_show)
            mLayoutApkCompiling!!.visibility = View.GONE
            mLayoutApkCompiled!!.visibility = View.VISIBLE
            mLayoutApkCompiled!!.startAnimation(animation)
            val pkg = AppUtils.getApkPackage(requireContext(), result.absolutePath)
            if (AppUtils.checkAppInstalled(requireContext(), pkg)) {
                mUninstallApp!!.visibility = View.VISIBLE
                mUninstallApp!!.setOnClickListener { v: View? ->
                    AppUtils.uninstallApp(requireContext(), pkg)
                }
            }
            mSavedFileMsg!!.text = requireContext().resources
                .getString(R.string.build_apk_saved_to, result.absolutePath)
            mInstallApp!!.setOnClickListener {
                AppUtils.installApk(requireContext(), result)
            }
        } else {
            mShowLog!!.visibility = View.VISIBLE
            mProgressBar!!.visibility = View.GONE
            mImageError!!.visibility = View.VISIBLE
            mProgressTip!!.setText(R.string.error_build_failed)
            mProgressTip!!.setTextColor(ContextCompat.getColor(requireContext(),R.color.google_red))
        }
        mShowLog!!.setOnClickListener {
            val fragment =
                FullLogDialogFragment.newInstance()
            fragment.show(parentFragmentManager, FullLogDialogFragment.TAG)
        }
        mCopyLog!!.setOnClickListener{StringUtils.setClipboard(requireContext(), mAdapter.errorLines.joinToString(), true)}
        mCloseFragment!!.setOnClickListener {
            requireContext().unbindService(connection)
            mBound = false
            stopBuildService()
            FragmentUtils.remove(this)
        }
    }

    private fun startBuildService() {
        val serviceIntent = Intent(requireContext(), BuildService::class.java)
        serviceIntent.putExtra("projectDir", mProjectDir)
        ContextCompat.startForegroundService(requireContext(), serviceIntent)
    }

    private fun stopBuildService() {
        val serviceIntent = Intent(requireContext(), BuildService::class.java)
        requireContext().stopService(serviceIntent)
    }

    private fun bindLocalService(){
        Intent(requireContext(), BuildService::class.java).also { intent ->
            requireContext().bindService(intent, connection, 0)
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as BuildService.LocalBinder
            mService = binder.getService()
            mBound = true

           /* //subscribe to compile log
            mService.compileLog.observe(viewLifecycleOwner, Observer {
                mLog!!.append(it + "\n")
                mLogSting!!.append(it + "\n")
            })*/

            //subscribe to errors
            mService.falied.observe(viewLifecycleOwner, Observer {
                initList(it)
            })
            //subscribe to steps
            mService.stepInfo.observe(viewLifecycleOwner, Observer {
                mProgressTip!!.text = it
            })
            //subscribe to build time
            mService.time.observe(viewLifecycleOwner, Observer {
                mCompiledTime!!.text = String.format(getString(R.string.app_compile_elapsed_time),
                    TimeUtils().formatStopWatchTime(it))
            })
            mService.success.observe(viewLifecycleOwner, Observer {
                mFinish = true
                builded(it)
              //  initList(mService.compileLog)
            })
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }

    override fun OnItemLongClick(message: String?) {
        StringUtils.setClipboard(requireContext(), message, true)
    }

    override fun OnItemClicked(filePath: String?, lineNumber: Int) {
        val fragment = FileOptionsDialogFragment.newInstance(filePath,lineNumber);
        fragment.show(parentFragmentManager,FileOptionsDialogFragment.TAG)
    }
}