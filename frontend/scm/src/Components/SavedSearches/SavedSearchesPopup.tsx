"use client";
import React, {useEffect, useState} from "react";
import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";
import {PredefinedSearch, PredefinedSearch as SavedSearchesModel} from "@/models/PredefinedSearch";
import {IconDefinition} from "@fortawesome/fontawesome-svg-core";
import {faArrowDown, faArrowUp} from "@fortawesome/free-solid-svg-icons";
import CreatableSelect from "react-select/creatable";
import {toast} from "react-toastify";

interface SavedSearchesPopupProps {
    icon: IconDefinition;
    title: string;
    IdToken: string;
    savedSearch: SavedSearchesModel;
    onSavedSearchAction: () => void;
    action: string;
}

const SavedSearchesPopup: React.FC<SavedSearchesPopupProps> = (props) => {
    const [showPopup, setShowPopup] = useState(false);
    const [savedSearch, setSavedSearch] = useState(props.savedSearch);
    const [availableTags, setAvailableTags] = useState<string[]>();

useEffect(() => {
    const fetchTags = async () => {
        try {
            const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/tenants/unique/${props.savedSearch.onTenant}`, {
                headers: {
                    'userToken': `Bearer ${props.IdToken}`,
                },
            });

            if (!res.ok) {
                throw new Error(`Error fetching tags: ${res.statusText}`);
            }

            const tenant = await res.json();
            const tags = Object.keys(tenant.contactTags).map(tag => (tag));
            setAvailableTags(tags);
        } catch (error) {
            console.error('Failed to fetch tags:', error);
        }
    };
        fetchTags();
    }, [props.savedSearch.onTenant, props.IdToken]);

    const handleTagsChange = (selectedOption: any) => {
        setSavedSearch(values => {
            return {...values, filter: selectedOption.map((option: any) => option.value)};
        });
    };

    const handleInputChange = (key: string, newValue: string) => {
        setSavedSearch(values => {
            return {...values, [key]: newValue};
        });
    };

    const handleDelete = async (savedSearchID: string, IdToken:  string) => {
        try {
            const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/predefined_searches/${savedSearchID}`, {
                method: 'DELETE',
                headers: {
                    'userToken': `Bearer ${IdToken}`,
                }
            });
            if (!res.ok) {
                toast.error("Error to delete search!");
                throw new Error(`Error deleting search: ${res.statusText}`);
            }
            toast.success("Search deleted successfully!");
            setTimeout(() => {
                props.onSavedSearchAction();
                setShowPopup(false);
            }, 2000);
        } catch (error) {
            toast.error("Failed to delete search!");
            console.error('Failed to delete search:', error);
        }

    }

    const handleEdit = async (savedSearch: PredefinedSearch, IdToken: string) => {
        try {
            const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/predefined_searches`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'userToken': `Bearer ${IdToken}`,
                },
                body: JSON.stringify(savedSearch),
            });
            if (!res.ok) {
                throw new Error(`Error editing search: ${res.statusText}`);
            }

            toast.success("Search edited successfully!");
            setTimeout(() => {
                props.onSavedSearchAction();
                setShowPopup(false);
            }, 2000);

        } catch (error) {
            toast.error("Failed to edit search!");
            console.error('Failed to edit search:', error);
        }
    }


    return (
        <div>
            <button onClick={() => setShowPopup(true)}
                    className={`btn px-4 btn-sm ${props.action === "delete" ? "bg-danger hover:bg-danger dark:bg-danger dark:hover:bg-danger" : "bg-primary-light hover:bg-primary-light"} text-white rounded-8 font-semibold hover:scale-105 transition`}
            >
                <FontAwesomeIcon className={"w-3.5 h-auto"} icon={props.icon}/>
            </button>
            {showPopup && (
                <div
                    className="fixed z-20 flex flex-col justify-center items-center bg-gray-500 bg-opacity-60 inset-0">
                    <div className="bg-white p-10 rounded-8 shadow-lg">
                        <h2 className={"font-semibold mb-4 text-2xl"}>{props.title}</h2>
                        {props.action === "delete" && (<p>Are you sure you want to delete this search?</p>)}
                        {props.action === "edit" && (
                            <div className={"p-2 justify-between flex flex-col items-start"}>
                                <label className={"font-normal mb-1"}>Title</label>
                                <p className={"text-lg font-semibold mb-2"}>
                                    {savedSearch.title}
                                </p>
                                <label className={"font-normal mb-1"}>On Tenant</label>
                                <p className={"text-lg font-semibold mb-2"}>
                                    {savedSearch.onTenant}
                                </p>
                                <label className={"font-normal mb-1"}>Search query</label>
                                <input
                                    className={"input input-bordered w-60 mb-2"}
                                    type="text"
                                    value={savedSearch.searchQuery}
                                    onChange={(e) => handleInputChange("searchQuery", e.target.value)}
                                />
                                <label className={"font-normal mb-1"}>Orientation</label>
                                <div className="flex items-center mb-2">
                                    <input
                                        type="radio"
                                        id="asc"
                                        name="orientation"
                                        value="ASC"
                                        checked={savedSearch.sortOrientation === 'ASC'}
                                        onChange={(e) => handleInputChange("sortOrientation", e.target.value)}
                                    />
                                    <label className="ml-2" htmlFor="asc">ASC <FontAwesomeIcon className="ml-1" icon={faArrowUp}/> </label>
                                </div>
                                <div className="flex items-center mb-2">
                                    <input
                                        type="radio"
                                        id="desc"
                                        name="orientation"
                                        value="DESC"
                                        checked={savedSearch.sortOrientation === 'DESC'}
                                        onChange={(e) => handleInputChange("sortOrientation", e.target.value)}
                                    />
                                    <label className="ml-2" htmlFor="desc">DESC <FontAwesomeIcon className="ml-1" icon={faArrowDown}/> </label>
                                </div>
                                <label className={"font-normal mb-1"}>Filter</label>
                                <CreatableSelect
                                    isMulti
                                    value={savedSearch.filter.map(tag => ({label: tag, value: tag}))}
                                    options={availableTags?.map(tag => ({label: tag, value: tag}))}
                                    onChange={handleTagsChange}
                                    className="shadow appearance-none border rounded w-60 py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
                                />
                            </div>
                        )}
                        <div className={"mt-4 justify-center items-center flex"}>
                            <button onClick={() => {
                                setShowPopup(false);
                            }}
                                    className="btn mt-4 mx-3 px-5 btn-sm bg-danger border-0 text-white rounded-8 font-semibold hover:bg-danger hover:scale-105 transition"
                            >Close Popup
                            </button>
                            {props.action === "delete" ?
                                <button onClick={() => handleDelete(savedSearch.id, props.IdToken)}
                                        className="btn mt-4 mx-3 px-5 btn-sm bg-primary-light border-0 text-white dark:bg-primary-dark dark:hover:bg-primary-dark rounded-8 font-semibold hover:bg-primary-light hover:scale-105 transition">
                                    Delete
                                </button> :
                                <button onClick={() => handleEdit(savedSearch, props.IdToken)}
                                    className="btn mt-4 mx-3 px-5 btn-sm bg-primary-light border-0 text-white dark:bg-primary-dark dark:hover:bg-primary-dark rounded-8 font-semibold hover:bg-primary-light hover:scale-105 transition">
                                    Save
                                </button>}
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
export default SavedSearchesPopup;