package hk.com.granda_express.gecollect;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.SharedPreferencesCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import static hk.com.granda_express.gecollect.R.id.editText;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OpearatorFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link OpearatorFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class OperatorFragment extends DialogFragment {
    Button button;

    private OnFragmentInteractionListener mListener;

    public OperatorFragment() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters
    public static OperatorFragment newInstance() {
        OperatorFragment fragment = new OperatorFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        getDialog().setTitle("请输入操作员");
        View view = inflater.inflate(R.layout.fragment_operator, container, false);
        ((EditText) view.findViewById(R.id.editOperator)).setText(mListener.getUserName());
        button = (Button) view.findViewById(R.id.button_confirm);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText wOperator = (EditText) getView().findViewById(R.id.editOperator);
                mListener.setUserName(wOperator.getText().toString());
                getDialog().dismiss();
            }
        });
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public String getUserName();
        public void setUserName(String userName);
    }


}
